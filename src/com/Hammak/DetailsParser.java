package com.Hammak;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class DetailsParser {

    private static final int ASSEMBLY_HALL_NUMBER = 403;
    // МЕГА КОСТЫЛЬ (МНЕ ЛЕНЬ ОПРЕДЕЛЯТЬ ГОД ИЗ ФАЙЛА)
    // влияет на LocalDate каждого дня (в getCurrentDayDate()),
    // то есть день недели тоже, так что для каждого года надо перекомпилировать проги. сук, надо переделать
    private static final int CURRENT_YEAR = 2018;
    private static Semester semester;
    private List<String> lines;

    DetailsParser(List<String> lines, Semester unfilledSemester) {
        this.lines = lines;
        semester = unfilledSemester;
        fillSemester();
    }

    static Semester getSemester() {
        return semester;
    }

    private static int getBlocksAmount(String line) {

        // |ауд.213 (01.03)|ауд.205 (08.03-15.03)|ауд.212 (22.03-12.04)|
        int blocksAmount = 0;

        for (int i = 0; i < line.length(); i++) {
            if (i == line.length() - 1) {
                if (line.charAt(line.length() - 1) == '|') {
                    return blocksAmount;
                }
            }
            if (line.charAt(i) == '|') {
                blocksAmount++;
            }
        }

        return blocksAmount;
    }

    private static int getSubjectEndIndex(String line) {
        int subjectEndIndex = -1;
        boolean endFound = false;
        int i = 2;
        while (!endFound) {
            if (line.charAt(i) == '(' && line.charAt(i + 2) == ')') {
                subjectEndIndex = i - 1;
                endFound = true;
            }
            i++;
        }
        return subjectEndIndex;
    }

    private void fillSemester() {

        replaceAllRussians();

        int i = 0;

        String line = lines.get(i);

        while (!(line.charAt(0) == ' ')) {
            if (Character.isLetter(line.charAt(0))) {
                DayOfWeek dayOfWeek = parseDayOfWeek(line);

                i++;
                line = lines.get(i);

                while (line.charAt(0) != '-') {
                    while (Character.isDigit(line.charAt(0))) {

                        // 0123456789
                        // 1 пара - 9:00
                        // 2 пара - 12:10
                        int pairNumber = Integer.parseInt(line.substring(0, 1));
                        LocalTime startTime = parseStartTime(line);

                        i++;
                        line = lines.get(i);

                        while (line.charAt(0) == '*') {
                            // 01234567890123456789012345678901234567890123456789
                            // * Інформаційні системи в обліку та аудиті (л) [ас. Духновська]
                            // * Корпоративні інформаційні системи (L) [доц. Сокульський]
                            //    |ауд.209 (05.03-19.03)
                            String subject = parseSubject(line);
                            String teacher = parseTeacher(line);

                            i++;
                            line = lines.get(i);

                            while (line.charAt(3) == '|') {

                                // 012345678901234567890123456789
                                //    |ауд.213 (01.03)|ауд.205 (08.03-15.03)|ауд.212 (22.03-12.04)|
                                //    |ауд.217 (19.04-26.04)
                                ArrayList<SomeDataStructure> someDataStructures = parseHardPart(line);
                                for (SomeDataStructure someDataStructure : someDataStructures) {
                                    fillPairs(pairNumber, startTime, subject, teacher,
                                            someDataStructure.getLectureHallNumber(),
                                            dayOfWeek,
                                            someDataStructure.getStartDay(),
                                            someDataStructure.getEndDay());
                                }

                                i++;
                                line = lines.get(i);
                            }
                        }

                    }
                }
            }
            i++;
            line = lines.get(i);
        }

    }

    private void replaceAllRussians() {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).charAt(0) == '*') {
                boolean flag = false;
                int j = 2;
                while (!flag) {
                    if (lines.get(i).charAt(j) == '(' && lines.get(i).charAt(j + 2) == ')') {
                        String newLine = lines.get(i).substring(0, j) + "(A" + lines.get(i).substring(j + 2, lines.get(i).length());
                        lines.remove(i);
                        lines.add(i, newLine);
                        flag = true;
                    }
                    j++;
                }
            }
        }
    }

    private ArrayList<SomeDataStructure> parseHardPart(String line) {

        ArrayList<SomeDataStructure> someDataStructures = new ArrayList<>();

        line = line.substring(3);
        // 012345678901234567890123456789
        // |ауд.213 (01.03)|ауд.205 (08.03-15.03)|ауд.212 (22.03-12.04)|
        // |ауд.217 (19.04-26.04)
        int blocksAmount = getBlocksAmount(line);

        for (int i = 0; i < blocksAmount; i++) {
            someDataStructures.add(new SomeDataStructure());

            // 012345678901234567890123456789
            // |ауд.213 (01.03)
            if (line.substring(5, 7).equals("АЗ")) {
                someDataStructures.get(i).setLectureHallNumber(DetailsParser.ASSEMBLY_HALL_NUMBER);
                line = line.substring(9);
            } else {
                someDataStructures.get(i).setLectureHallNumber(Integer.parseInt(line.substring(5, 8)));
                line = line.substring(10);
            }

            // 012345678901234567890123456789
            // 01.03)
            // 19.04-26.04)
            int startDayDay = Integer.parseInt(line.substring(0, 2));
            int startDayMonth = Integer.parseInt(line.substring(3, 5));
            LocalDate startDay = LocalDate.of(CURRENT_YEAR, startDayMonth, startDayDay);
            someDataStructures.get(i).setStartDay(startDay);


            if (line.charAt(5) == '-') {
                line = line.substring(6);
                // 0123456789
                // 26.04)

                int endDayDay = Integer.parseInt(line.substring(0, 2));
                int endDayMonth = Integer.parseInt(line.substring(3, 5));
                someDataStructures.get(i).setEndDay(LocalDate.of(CURRENT_YEAR, endDayMonth, endDayDay));
                line = line.substring(6);
            } else {
                someDataStructures.get(i).setEndDay(startDay);
                line = line.substring(6);
            }

        }

        return someDataStructures;
    }

    private String parseTeacher(String line) {

        // 01234567890123456789
        // * Корпоративні інформаційні системи (L) [доц. Сокульський]
        int openingSquareBracketIndex = line.indexOf('[');
        int closingSquareBracketIndex = line.indexOf(']');

        return line.substring(openingSquareBracketIndex + 1, closingSquareBracketIndex);
    }

    private String parseSubject(String line) {

        // 0123456789012345678901234567890123456789
        // * Корпоративні інформаційні системи (L) [доц. Сокульський]
        int subjectEndIndex = getSubjectEndIndex(line);

        return line.substring(2, subjectEndIndex);
    }

    private LocalTime parseStartTime(String line) {

        // 0123456789
        // 1 пара - 9:00
        // 2 пара - 12:10
        int doubleDotIndex = line.indexOf(':');
        int startTimeHours = Integer.parseInt(line.substring(9, doubleDotIndex));
        int startTimeMinutes = Integer.parseInt(line.substring(doubleDotIndex + 1));

        return LocalTime.of(startTimeHours, startTimeMinutes);
    }

    private DayOfWeek parseDayOfWeek(String line) {

        // 0123456789
        // Понеділок
        // Вівторок

        HashMap<String, DayOfWeek> weekDays = new HashMap<>() {{
            put("Понеділок", DayOfWeek.MONDAY);
            put("Вівторок", DayOfWeek.TUESDAY);
            put("Середа", DayOfWeek.WEDNESDAY);
            put("Четвер", DayOfWeek.THURSDAY);
            put("П\"ятниця", DayOfWeek.FRIDAY);
            put("Субота", DayOfWeek.SATURDAY);
        }};

        return weekDays.get(line);
    }

    private void fillPairs(int pairNumber, LocalTime startTime, String subject, String teacher, int lectureHallNumber,
                           DayOfWeek dayOfWeek, LocalDate startDay, LocalDate endDay) {

        for (int i = 0; i < semester.weeksAmount(); i++) {
            for (int j = 0; j < semester.getWeek(i).daysAmount(); j++) {
                if (semester.getWeek(i).getDay(j).getDate().isAfter(startDay.minusDays(1))) {

                    if (semester.getWeek(i).getDay(j).getDate().getDayOfWeek() == dayOfWeek) {
                        semester.getWeek(i).getDay(j).fillPair(pairNumber, startTime, lectureHallNumber, subject, teacher);
                    }

                    if (semester.getWeek(i).getDay(j).getDate().isAfter(endDay.plusDays(1))) {
                        return;
                    }
                }
            }
        }
    }
}
