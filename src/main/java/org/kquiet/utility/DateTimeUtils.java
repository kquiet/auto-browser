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
import java.util.AbstractMap.SimpleEntry;
import java.util.List;

/**
 *
 * @author Kimberly
 */
public final class DateTimeUtils {
    private DateTimeUtils(){}
    
    /**
     *
     * @param format
     * @return
     */
    public static String nowStr(String format){
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(format));
    }

    /**
     *
     * @param format
     * @return
     */
    public static String utcNowStr(String format){
        return LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern(format));
    }

    /**
     *
     * @param d
     * @param format
     * @return
     */
    public static String toStr(LocalDateTime d, String format){
        if (d==null) return "";
        return d.format(DateTimeFormatter.ofPattern(format));
    }

    /**
     *
     * @param d
     * @param format
     * @return
     */
    public static String toStr(ZonedDateTime d, String format){
        if (d==null) return "";
        return d.format(DateTimeFormatter.ofPattern(format));
    }

    /**
     *
     * @param d
     * @param format
     * @return
     */
    public static String toStr(LocalDate d, String format){
        if (d==null) return "";
        return d.format(DateTimeFormatter.ofPattern(format));
    }

    /**
     *
     * @param d
     * @param format
     * @return
     */
    public static String toStr(LocalTime d, String format){
        if (d==null) return "";
        return d.format(DateTimeFormatter.ofPattern(format));
    }
    
    /**
     *
     * @param start
     * @param end
     * @param time
     * @return
     */
    public static boolean isBetween(LocalTime start, LocalTime end, LocalTime time) {
        if (start.equals(end)){
            return true;
        }
        else if (start.isAfter(end)){
            return !time.isBefore(start) || !time.isAfter(end);
        }
        else{
            return !time.isBefore(start) && !time.isAfter(end);
        }
    }
    
    /**
     *
     * @param start
     * @param end
     * @param from
     * @return
     */
    public static LocalDateTime nextFire(LocalTime start, LocalTime end, LocalDateTime from){
        LocalDate fromDate = from.toLocalDate();
        LocalTime fromTime = from.toLocalTime();
        if (start.equals(end)){
            return from;
        }
        else if (start.isAfter(end)){
            if (fromTime.isBefore(start) && fromTime.isAfter(end)){
                return LocalDateTime.of(fromDate, start);
            }
            else{
                return from;
            }
        }
        else{
            if (!fromTime.isBefore(start) && !fromTime.isAfter(end)){
                return from;
            }
            else if (fromTime.isBefore(start)){
                return LocalDateTime.of(fromDate, start);
            }
            else{
                return LocalDateTime.of(fromDate.plusDays(1), start);
            }
        }
    }
    
    /**
     *
     * @param conditionList
     * @param from
     * @return
     */
    public static LocalDateTime nextFire(List<SimpleEntry<LocalTime,LocalTime>> conditionList, LocalDateTime from){
        LocalDateTime candidate = LocalDateTime.MAX;
        for(SimpleEntry<LocalTime,LocalTime> condition:conditionList){
            LocalDateTime nextFire = nextFire(condition.getKey(), condition.getValue(), from);
            if (nextFire.isBefore(candidate)){
                candidate = nextFire;
            }
        }
        return candidate;
    }
}
