package com.gridnine.testing;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.time.Duration;

/**
 * Factory class to get sample list of flights.
 */
class FlightBuilder {
    static List<Flight> createFlights() {
        LocalDateTime threeDaysFromNow = LocalDateTime.now().plusDays(3);
        return Arrays.asList(
                //A normal flight with two hour duration
                createFlight(threeDaysFromNow, threeDaysFromNow.plusHours(2)),
                //A normal multi segment flight
                createFlight(threeDaysFromNow, threeDaysFromNow.plusHours(2),
                        threeDaysFromNow.plusHours(3), threeDaysFromNow.plusHours(5)),
                //A flight departing in the past
                createFlight(threeDaysFromNow.minusDays(6), threeDaysFromNow),
                //A flight that departs before it arrives
                createFlight(threeDaysFromNow, threeDaysFromNow.minusHours(6)),
                //A flight with more than two hours ground time
                createFlight(threeDaysFromNow, threeDaysFromNow.plusHours(2),
                        threeDaysFromNow.plusHours(5), threeDaysFromNow.plusHours(6)),
                //Another flight with more than two hours ground time
                createFlight(threeDaysFromNow, threeDaysFromNow.plusHours(2),
                        threeDaysFromNow.plusHours(3), threeDaysFromNow.plusHours(4),
                        threeDaysFromNow.plusHours(6), threeDaysFromNow.plusHours(7)));
    }

    private static Flight createFlight(final LocalDateTime... dates) {
        if ((dates.length % 2) != 0) {
            throw new IllegalArgumentException(
                    "you must pass an even number of dates");
        }
        List<Segment> segments = new ArrayList<>(dates.length / 2);
        for (int i = 0; i < (dates.length - 1); i += 2) {
            segments.add(new Segment(dates[i], dates[i + 1]));
        }
        return new Flight(segments);
    }
}

/**
 * Bean that represents a flight.
 */
class Flight {
    private final List<Segment> segments;

    Flight(final List<Segment> segs) {
        segments = segs;
    }

    List<Segment> getSegments() {
        return segments;
    }

    @Override
    public String toString() {
        return segments.stream().map(Object::toString)
                .collect(Collectors.joining(" "));
    }
}

/**
 * Bean that represents a flight segment.
 */
class Segment {
    private final LocalDateTime departureDate;

    private final LocalDateTime arrivalDate;

    Segment(final LocalDateTime dep, final LocalDateTime arr) {
        departureDate = Objects.requireNonNull(dep);
        arrivalDate = Objects.requireNonNull(arr);
    }

    LocalDateTime getDepartureDate() {
        return departureDate;
    }

    LocalDateTime getArrivalDate() {
        return arrivalDate;
    }

    @Override
    public String toString() {
        DateTimeFormatter fmt =
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        return '[' + departureDate.format(fmt) + '|' + arrivalDate.format(fmt)
                + ']';
    }
}

interface FlightFilter {
    boolean isValid(Flight flight);
}

class DepartureBeforeNowFilter implements FlightFilter {
    @Override
    public boolean isValid(Flight flight) {
        LocalDateTime now = LocalDateTime.now();
        for (Segment segment : flight.getSegments()) {
            if (segment.getDepartureDate().isBefore(now)) {
                return false;
            }
        }
        return true;
    }
}

class ArrivalBeforeDepartureFilter implements FlightFilter {
    @Override
    public boolean isValid(Flight flight) {
        for (Segment segment : flight.getSegments()) {
            if (segment.getArrivalDate().isBefore(segment.getDepartureDate())) {
                return false;
            }
        }
        return true;
    }
}

class GroundTimeFilter implements FlightFilter {
    @Override
    public boolean isValid(Flight flight) {
        List<Segment> segments = flight.getSegments();
        long totalGroundMinutes = 0;
        for (int i = 0; i < segments.size() - 1; i++) {
            LocalDateTime arrival = segments.get(i).getArrivalDate();
            LocalDateTime nextDeparture = segments.get(i + 1).getDepartureDate();
            totalGroundMinutes += Duration.between(arrival, nextDeparture).toMinutes();
        }
        return totalGroundMinutes <= 120;
    }
}

class FlightFilterService {

    static List<Flight> filter(List<Flight> flights, FlightFilter filter) {
        List<Flight> result = new ArrayList<>();
        for (Flight flight : flights) {
            if (filter.isValid(flight)) {
                result.add(flight);
            }
        }
        return result;
    }

    static List<Flight> filterMulti(List<Flight> flights, List<FlightFilter> filters) {
        List<Flight> result = new ArrayList<>(flights);
        for (FlightFilter filter : filters) {
            result = filter(result, filter);
        }
        return result;
    }
}

public class Main {
    public static void main(String[] args) {
        List<Flight> flights = FlightBuilder.createFlights();

        System.out.println("Все перелёты");
        flights.forEach(System.out::println);

        System.out.println("Исключаем перелёты с вылетом до текущего момента");
        FlightFilterService.filter(flights, new DepartureBeforeNowFilter()).forEach(System.out::println);

        System.out.println("Исключаем сегменты с прилётом раньше вылета");
        FlightFilterService.filter(flights, new ArrivalBeforeDepartureFilter()).forEach(System.out::println);

        System.out.println("Исключаем перелёты с более чем 2 часами на земле");
        FlightFilterService.filter(flights, new GroundTimeFilter()).forEach(System.out::println);

        System.out.println("Перелёты, прошедшие ВСЕ фильтры");
        List<FlightFilter> filters = List.of(
                new DepartureBeforeNowFilter(),
                new ArrivalBeforeDepartureFilter(),
                new GroundTimeFilter()
        );
        FlightFilterService.filterMulti(flights, filters).forEach(System.out::println);
    }
}