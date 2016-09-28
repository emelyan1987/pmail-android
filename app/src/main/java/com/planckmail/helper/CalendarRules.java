package com.planckmail.helper;

/**
 * Created by Taras Matolinets on 21.06.15.
 */
public class CalendarRules {


    public enum RULE {
        BY_DAY("BYDAY"), RRULE("RRULE"), FREQ("FREQ");

        private final String rule;

        RULE(String rule) {
            this.rule = rule;
        }

        public String getString() {
            return rule;
        }
    }


    public enum PERIOD {
        DAILY("DAILY"), MONTHLY("MONTHLY"), WEEKLY("WEEKLY");

        private final String period;

        PERIOD(String period) {
            this.period = period;
        }

        public String getString() {
            return period;
        }
    }

    public enum DAYS_CALENDAR {
        MONDAY("MO"), TUESDAY("TU"), WEDNESDAY("WE"), THURSDAY("TH"), FRIDAY("FR");

        private final String day;

        DAYS_CALENDAR(String day) {
            this.day = day;
        }

        public String getString() {
            return day;
        }
    }
}
