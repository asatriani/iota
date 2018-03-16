package com.italtel.iota.demo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.quartz.CronExpression;

public class CronUtils {

    public static Date getNextFireTime(String cronExpr, Date fromDate) {
        try {
            CronExpression cronExpression = new CronExpression(cronExpr);
            return cronExpression.getNextValidTimeAfter(fromDate);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public static List<Date> getNextFireTimes(String cronExpr, Date fromDate, Date toDate) {
        try {
            CronExpression cronExpression = new CronExpression(cronExpr);
            List<Date> result = new ArrayList<>();
            Date from = (Date) fromDate.clone();
            while (from.before(toDate)) {
                from = cronExpression.getNextValidTimeAfter(from);
                result.add(from);
            }
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public static Date getPreviousFireTime(String cronExpr, Date fromDate) {
        try {
            CronExpression cronExpression = new CronExpression(cronExpr);
            Date nextFireTime = cronExpression.getNextValidTimeAfter(fromDate);
            Date subsequentFireTime = cronExpression.getNextValidTimeAfter(nextFireTime);
            long interval = subsequentFireTime.getTime() - nextFireTime.getTime();
            return new Date(nextFireTime.getTime() - interval);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

}
