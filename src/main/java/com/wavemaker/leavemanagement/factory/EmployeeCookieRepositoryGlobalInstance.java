package com.wavemaker.leavemanagement.factory;

import com.wavemaker.leavemanagement.repository.EmployeeCookieRepository;
import com.wavemaker.leavemanagement.repository.impl.indb.EmployeeCookieRepositoryImpl;

public class EmployeeCookieRepositoryGlobalInstance {
    private static EmployeeCookieRepository employeeCookieRepository;

    public static EmployeeCookieRepository getEmployeeCookieRepositoryInstance() {
        if (employeeCookieRepository == null) {
            synchronized (EmployeeCookieRepositoryGlobalInstance.class) {
                if (employeeCookieRepository == null) {
                    employeeCookieRepository = new EmployeeCookieRepositoryImpl();

                }


            }

        }
        return employeeCookieRepository;
    }
}
