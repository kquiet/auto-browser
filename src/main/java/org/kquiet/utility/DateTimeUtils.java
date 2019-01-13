/*
 * Copyright 2018 kquiet.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kquiet.utility;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Date and time utility class
 * @author Kimberly
 */
public final class DateTimeUtils {
    private DateTimeUtils(){}
    
    /**
     * Get current time in specified in system default timezone.
     * 
     * @param format time format
     * @return current time
     */
    public static String nowStr(String format){
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(format));
    }

    /**
     * Get current time in specified in UTC timezone.
     * 
     * @param format time format
     * @return current time
     */
    public static String utcNowStr(String format){
        return LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern(format));
    }

    /**
     * Format {@link LocalDateTime} in specified format.
     * 
     * @param localDateTime {@link LocalDateTime} to format
     * @param format time format
     * @return formatted {@link LocalDateTime}
     */
    public static String toStr(LocalDateTime localDateTime, String format){
        if (localDateTime==null) return "";
        return localDateTime.format(DateTimeFormatter.ofPattern(format));
    }

    /**
     * Format {@link ZonedDateTime} in specified format.
     * 
     * @param zonedDateTime {@link ZonedDateTime} to format
     * @param format time format
     * @return formatted {@link ZonedDateTime}
     */
    public static String toStr(ZonedDateTime zonedDateTime, String format){
        if (zonedDateTime==null) return "";
        return zonedDateTime.format(DateTimeFormatter.ofPattern(format));
    }

    /**
     * Format {@link LocalDate} in specified format.
     * 
     * @param localDate {@link LocalDate} to format
     * @param format time format
     * @return formatted {@link LocalDate}
     */
    public static String toStr(LocalDate localDate, String format){
        if (localDate==null) return "";
        return localDate.format(DateTimeFormatter.ofPattern(format));
    }

    /**
     * Format {@link LocalTime} in specified format.
     * 
     * @param localTime {@link LocalTime} to format
     * @param format time format
     * @return formatted {@link LocalTime}
     */
    public static String toStr(LocalTime localTime, String format){
        if (localTime==null) return "";
        return localTime.format(DateTimeFormatter.ofPattern(format));
    }
}
