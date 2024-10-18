package com.wavemaker.leavemanagement.factory;

import com.wavemaker.leavemanagement.repository.EmployeeLeaveSummaryRepository;
import com.wavemaker.leavemanagement.repository.impl.indb.EmployeeLeaveSummaryRepositoryImpl;

public class EmployeeLeaveSummaryRepositoryGlobalInstance {
    private static EmployeeLeaveSummaryRepository employeeLeaveSummaryRepository = null;

    public static EmployeeLeaveSummaryRepository getEmployeeLeaveSummaryRepositoryInstance() {
        if (employeeLeaveSummaryRepository == null) {
            synchronized (EmployeeLeaveSummaryRepositoryGlobalInstance.class) {
                if (employeeLeaveSummaryRepository == null) {
                    employeeLeaveSummaryRepository = new EmployeeLeaveSummaryRepositoryImpl();
                }
            }

        }
        return employeeLeaveSummaryRepository;
    }

}
