package com.domainify.dto;

public class BusinessHoursWeekDto {

    private BusinessHoursDayDto monday;
    private BusinessHoursDayDto tuesday;
    private BusinessHoursDayDto wednesday;
    private BusinessHoursDayDto thursday;
    private BusinessHoursDayDto friday;
    private BusinessHoursDayDto saturday;
    private BusinessHoursDayDto sunday;

    public BusinessHoursDayDto getMonday() {
        return monday;
    }

    public void setMonday(BusinessHoursDayDto monday) {
        this.monday = monday;
    }

    public BusinessHoursDayDto getTuesday() {
        return tuesday;
    }

    public void setTuesday(BusinessHoursDayDto tuesday) {
        this.tuesday = tuesday;
    }

    public BusinessHoursDayDto getWednesday() {
        return wednesday;
    }

    public void setWednesday(BusinessHoursDayDto wednesday) {
        this.wednesday = wednesday;
    }

    public BusinessHoursDayDto getThursday() {
        return thursday;
    }

    public void setThursday(BusinessHoursDayDto thursday) {
        this.thursday = thursday;
    }

    public BusinessHoursDayDto getFriday() {
        return friday;
    }

    public void setFriday(BusinessHoursDayDto friday) {
        this.friday = friday;
    }

    public BusinessHoursDayDto getSaturday() {
        return saturday;
    }

    public void setSaturday(BusinessHoursDayDto saturday) {
        this.saturday = saturday;
    }

    public BusinessHoursDayDto getSunday() {
        return sunday;
    }

    public void setSunday(BusinessHoursDayDto sunday) {
        this.sunday = sunday;
    }
}
