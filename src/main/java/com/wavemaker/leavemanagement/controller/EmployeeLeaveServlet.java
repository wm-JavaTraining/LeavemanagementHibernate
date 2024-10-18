package com.wavemaker.leavemanagement.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wavemaker.leavemanagement.constants.LeaveRequestStatus;
import com.wavemaker.leavemanagement.exception.ServerUnavailableException;
import com.wavemaker.leavemanagement.model.Employee;
import com.wavemaker.leavemanagement.model.EmployeeLeave;
import com.wavemaker.leavemanagement.model.EmployeeLeaveSummary;
import com.wavemaker.leavemanagement.model.LeaveRequest;
import com.wavemaker.leavemanagement.service.EmployeeLeaveService;
import com.wavemaker.leavemanagement.service.EmployeeLeaveSummaryService;
import com.wavemaker.leavemanagement.service.EmployeeService;
import com.wavemaker.leavemanagement.service.impl.EmployeeLeaveServiceImpl;
import com.wavemaker.leavemanagement.service.impl.EmployeeLeaveSummaryServiceImpl;
import com.wavemaker.leavemanagement.service.impl.EmployeeServiceImpl;
import com.wavemaker.leavemanagement.util.LocalDateAdapter;
import com.wavemaker.leavemanagement.util.LocalTimeAdapter;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@WebServlet("/employee/leave/*")
public class EmployeeLeaveServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeLeaveServlet.class);
    private static Gson gson;
    private static EmployeeLeaveService employeeLeaveService;
    private static EmployeeService employeeService;
    private static EmployeeLeaveSummaryService employeeLeaveSummaryService;
    private static ApplicationContext context;

    @Override
    public void init() {
        context = new ClassPathXmlApplicationContext("configmetadata.xml");
        employeeLeaveService =  context.getBean(EmployeeLeaveService.class);
        employeeService =  context.getBean(EmployeeService.class);
        employeeLeaveSummaryService = context.getBean(EmployeeLeaveSummaryServiceImpl.class);
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .registerTypeAdapter(LocalTime.class, new LocalTimeAdapter())
                .create();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.equals("/getAppliedLeaves")) {
            getAppliedLeaves(request, response);
        } else if (pathInfo != null && pathInfo.equals("/getMyTeamRequests")) {
            getMyTeamRequests(request, response);

        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writeResponse(response, "The requested resource [" + pathInfo + "] is not available.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.equals("/applyEmployeeLeave")) {
            applyEmployeeLeave(request, response);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.equals("/acceptLeaveRequest")) {
            acceptLeaveRequest(request, response);
        } else if (pathInfo != null && pathInfo.equals("/rejectLeaveRequest")) {
            rejectLeaveRequest(request, response);
        }
    }

    private void applyEmployeeLeave(HttpServletRequest request, HttpServletResponse response) {
        String leaveType = request.getParameter("leaveType");
        String jsonResponse = "";
        Employee employee = null;
        if (leaveType == null || leaveType.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeResponse(response, "Leave type is required.");
            return;
        }
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                writeResponse(response, "User is not logged");
                return;
            }
            Integer loginId = (Integer) session.getAttribute("loginId");
            if (loginId == null) {
                writeResponse(response, "User is not present.");
                return;
            }
            // Read the request body once
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
            }
            String requestBody = stringBuilder.toString();
            EmployeeLeave employeeLeave = gson.fromJson(requestBody, EmployeeLeave.class);
            LeaveRequest leaveRequest = gson.fromJson(requestBody, LeaveRequest.class);
            employee = employeeService.getEmployeeByLoginId(loginId);
            if (employee == null) {
                writeResponse(response, "Employee not found.");
                return;
            }
//            int leaveTypeId = employeeLeaveService.getLeaveTypeId(leaveType);
//            leaveRequest.setLeaveTypeId(leaveTypeId);
//            employeeLeave.setLeaveTypeId(leaveTypeId);
//            employeeLeave.setLeaveType(leaveType);
//            leaveRequest.setEmployeeId(employee.getEmployeeId());
//            leaveRequest.setManagerId(employee.getManagerId());
//            employeeLeave.setEmployeeId(employee.getEmployeeId());
//            employeeLeave.setManagerId(employee.getManagerId());
//
//
//            leaveRequest.setStatus("PENDING");
//            // Apply the leave request
            int numberOfLeavesAllocated = employeeLeaveService.getNumberOfLeavesAllocated(leaveType);

            logger.info("Final Leave limit for type '{}' is: {} ",leaveType, numberOfLeavesAllocated);
            employeeLeave.setTypeLimit(numberOfLeavesAllocated);
            int totalNumberOfLeavesTaken = employeeLeaveService.getTotalNumberOfLeavesTaken(employee.getEmployeeId(), leaveRequest.getLeaveTypeId());
            logger.info("Total Leaves Taken: {}", totalNumberOfLeavesTaken);
            employeeLeave.setTotalEmployeeLeavesTaken(totalNumberOfLeavesTaken);
            LeaveRequest addLeaveRequest = employeeLeaveService.applyLeave(employeeLeave);
            if (addLeaveRequest == null) {
                writeResponse(response, "Failed to apply leave request.");
                return;
            }
            EmployeeLeaveSummary employeeSummary = new EmployeeLeaveSummary();
            employeeSummary.setEmployeeId(addLeaveRequest.getEmployeeId());
            employeeSummary.setLeaveType(leaveType);
            employeeSummary.setLeaveTypeId(addLeaveRequest.getLeaveTypeId());
            employeeSummary.setTotalAllocatedLeaves(numberOfLeavesAllocated);
            employeeSummary.setTotalLeavesTaken(totalNumberOfLeavesTaken);
            employeeLeaveSummaryService.addEmployeeLeaveSummary(employeeSummary);
            jsonResponse = gson.toJson(employeeLeave);
            writeResponse(response, jsonResponse);


        } catch (ServerUnavailableException e) {
            jsonResponse = "Service temporarily unavailable. Please try again later.";
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeResponse(response, jsonResponse);
        } catch (Exception e) {
            jsonResponse = "An error occurred while inserting the leave request: \" + e.getMessage()";
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeResponse(response, jsonResponse);

        }
    }

    private void getAppliedLeaves(HttpServletRequest request, HttpServletResponse response) {
        String status = request.getParameter("status");
        String jsonResponse = "";
        if (status == null || status.isEmpty()) {
            writeResponse(response, "Status is empty");
            return;
        }
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Integer loginId = (Integer) session.getAttribute("loginId");
                if (loginId != null) {
                    Employee employee = employeeService.getEmployeeByLoginId(loginId);
                    if (employee != null) {
                        int employeeId = employee.getEmployeeId();
                        List<EmployeeLeave> leaveRequests = employeeLeaveService.getAppliedLeaves(employeeId, LeaveRequestStatus.valueOf(status));
                        jsonResponse = gson.toJson(leaveRequests);
                        writeResponse(response, jsonResponse);
                    } else {
                        writeResponse(response, "Employee not found.");
                    }
                } else {
                    writeResponse(response, "User is not present.");
                }
            } else {
                writeResponse(response, "Session is not valid.");
            }
        } catch (ServerUnavailableException e) {
            jsonResponse = "Service temporarily unavailable. Please try again later.";
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeResponse(response, jsonResponse);
        } catch (Exception e) {
            jsonResponse = "An error occurred while fetching leaves for the logged-in employee: " + e.getMessage();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeResponse(response, jsonResponse);

        }
    }

    private void getMyTeamRequests(HttpServletRequest request, HttpServletResponse response) {
        String status = request.getParameter("status");
        Employee employee = null;
        String jsonResponse = "";
        if (status == null || status.isEmpty()) {
            writeResponse(response, "status is empty");
            return;
        }
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Integer loginId = (Integer) session.getAttribute("loginId");
                if (loginId != null) {
                    employee = employeeService.getEmployeeByLoginId(loginId);
                    int managerId = employee.getEmployeeId();
                    List<Integer> employeeIds = employeeService.getEmpIdUnderManager(managerId);
                    List<EmployeeLeave> employeeLeaves = employeeLeaveService.getLeavesOfEmployees(employeeIds, LeaveRequestStatus.valueOf(status));
                    jsonResponse = gson.toJson(employeeLeaves);
                    writeResponse(response, jsonResponse);
                } else {
                    writeResponse(response, "Manager ID is missing.");
                }
            }

        } catch (ServerUnavailableException e) {
            jsonResponse = "Service temporarily unavailable. Please try again later.";
            writeResponse(response, jsonResponse);
        } catch (Exception e) {
            jsonResponse = "An error occurred while retrieving team leave requests: " + e.getMessage();
            writeResponse(response, jsonResponse);

        }

    }

    private void acceptLeaveRequest(HttpServletRequest request, HttpServletResponse response) {
        EmployeeLeave employeeLeave = null;
        String jsonResponse;
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Integer loginId = (Integer) session.getAttribute("loginId");
                if (loginId != null) {
                    String leaveIdStr = request.getParameter("leaveId");
                    if (leaveIdStr != null) {
                        leaveIdStr = leaveIdStr.trim();
                        int leaveId = Integer.parseInt(leaveIdStr);
                        employeeLeave = employeeLeaveService.acceptLeaveRequest(leaveId);
                        if (employeeLeave != null) {
                            EmployeeLeaveSummary employeeLeaveSummary = new EmployeeLeaveSummary();
                            String leaveType = employeeLeaveService.getLeaveType(employeeLeave.getLeaveTypeId());
                            employeeLeaveSummary.setLeaveType(leaveType);
                            employeeLeaveSummary.setLeaveTypeId(employeeLeave.getLeaveTypeId());
                            employeeLeaveSummary.setEmployeeId(employeeLeave.getEmployeeId());
                            int numberOfLeavesAllocated = employeeLeaveService.getNumberOfLeavesAllocated(leaveType);
                            employeeLeaveSummary.setTotalAllocatedLeaves(numberOfLeavesAllocated);
                            int totalNumberOfLeavesTaken = employeeLeaveService.getTotalNumberOfLeavesTaken(employeeLeave.getEmployeeId(), employeeLeave.getLeaveTypeId());
                            logger.info("Total Leaves Taken: {}", totalNumberOfLeavesTaken);
                            employeeLeaveSummary.setTotalLeavesTaken(totalNumberOfLeavesTaken);
                            employeeLeaveSummaryService.updateEmployeeLeaveSummary(employeeLeaveSummary);
                            jsonResponse = gson.toJson(employeeLeave);
                            writeResponse(response, jsonResponse);
                        } else {
                            writeResponse(response, "Leave request not found or already accepted.");
                        }
                    } else {
                        writeResponse(response, "Leave ID is missing.");
                    }
                } else {
                    writeResponse(response, "User is not present.");
                }
            } else {
                writeResponse(response, "Session is not valid.");
            }
        } catch (ServerUnavailableException e) {
            jsonResponse = "Service temporarily unavailable. Please try again later.";
            writeResponse(response, jsonResponse);
        } catch (Exception e) {
            jsonResponse = "An error occurred while accepting the leave request: " + e.getMessage();
            writeResponse(response, jsonResponse);

        }
    }

    private void rejectLeaveRequest(HttpServletRequest request, HttpServletResponse response) {
        LeaveRequest leaveRequest = null;
        String jsonResponse;
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Integer loginId = (Integer) session.getAttribute("loginId");
                if (loginId != null) {
                    String leaveIdStr = request.getParameter("leaveId");
                    if (leaveIdStr != null) {
                        leaveIdStr = leaveIdStr.trim();
                        int leaveId = Integer.parseInt(leaveIdStr);
                        leaveRequest = employeeLeaveService.rejectLeaveRequest(leaveId);
                        if (leaveRequest != null) {
                            jsonResponse = gson.toJson(leaveRequest);
                            writeResponse(response, jsonResponse);
                        } else {
                            writeResponse(response, "Leave request not found.");
                        }
                    } else {
                        writeResponse(response, "Leave ID is missing.");
                    }
                } else {
                    writeResponse(response, "User is not present.");
                }
            } else {
                writeResponse(response, "Session is not valid.");
            }
        } catch (ServerUnavailableException e) {
            jsonResponse = "Service temporarily unavailable. Please try again later.";
            writeResponse(response, jsonResponse);
        } catch (Exception e) {
            jsonResponse = "An error occurred while rejecting the leave request: " + e.getMessage();
            writeResponse(response, jsonResponse);

        }
    }

    private void writeResponse(HttpServletResponse response, String jsonResponse) {
        PrintWriter printWriter = null;
        try {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            printWriter = response.getWriter();
            printWriter.print(jsonResponse);
            printWriter.flush();
        } catch (IOException e) {
            jsonResponse = "server Unavailable";
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            printWriter.print(jsonResponse);
            printWriter.flush();

        }
    }
}
