package com.gridnine.testing;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FlightFilterTest {

    @Test
    void testDepartureBeforeNowFilter() {
        List<Flight> flights = List.of(
                new Flight(List.of(new Segment(
                        LocalDateTime.now().plusHours(1),
                        LocalDateTime.now().plusHours(2)
                ))),
                new Flight(List.of(new Segment(
                        LocalDateTime.now().minusHours(1),
                        LocalDateTime.now().plusHours(1)
                )))
        );

        DepartureBeforeNowFilter filter = new DepartureBeforeNowFilter();
        assertTrue(filter.isValid(flights.get(0)), "Вылет в будущем должен пройти фильтр");
        assertFalse(filter.isValid(flights.get(1)), "Вылет в прошлом должен быть отклонён");
    }

    @Test
    void testArrivalBeforeDepartureFilter() {
        List<Flight> flights = List.of(
                new Flight(List.of(new Segment(LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2)))),
                new Flight(List.of(new Segment(LocalDateTime.now().plusHours(3), LocalDateTime.now().plusHours(1)
                )))
        );

        ArrivalBeforeDepartureFilter filter = new ArrivalBeforeDepartureFilter();
        assertTrue(filter.isValid(flights.get(0)), "Прилёт позже вылета — корректный сегмент");
        assertFalse(filter.isValid(flights.get(1)), "Прилёт раньше вылета — ошибка");
    }

    @Test
    void testExcessiveGroundTimeFilter() {
        LocalDateTime now = LocalDateTime.now();

        List<Flight> flights = List.of(
                new Flight(List.of(new Segment(now, now.plusHours(1)),
                        new Segment(now.plusHours(1).plusMinutes(30), now.plusHours(3)))),
                new Flight(List.of(new Segment(now, now.plusHours(1)), new Segment(now.plusHours(4), now.plusHours(5))
                ))
        );

        GroundTimeFilter filter = new GroundTimeFilter();
        assertTrue(filter.isValid(flights.get(0)), "Пересадка 30 минут — допустимо");
        assertFalse(filter.isValid(flights.get(1)), "Пересадка 3 часа — превышение нормы");
    }

    @Test
    void testFilterChainAppliesAllFiltersCorrectly() {
        LocalDateTime now = LocalDateTime.now();

        Flight validFlight = new Flight(List.of(new Segment(now.plusHours(1), now.plusHours(2))));

        Flight flightWithPastDeparture = new Flight(List.of(new Segment(now.minusHours(1), now.plusHours(1))));

        Flight flightWithWrongArrival = new Flight(List.of(new Segment(now.plusHours(3), now.plusHours(1))));

        Flight flightWithLongGroundTime = new Flight(List.of(new Segment(now.plusHours(1), now.plusHours(2)),
                new Segment(now.plusHours(5), now.plusHours(6))));

        List<Flight> flights = List.of(validFlight, flightWithPastDeparture, flightWithWrongArrival, flightWithLongGroundTime);

        List<FlightFilter> filters = List.of(
                new DepartureBeforeNowFilter(),
                new ArrivalBeforeDepartureFilter(),
                new GroundTimeFilter()
        );

        List<Flight> result = FlightFilterService.filterMulti(flights, filters);

        assertEquals(1, result.size(), "Должен остаться только один валидный перелёт");
        assertEquals(validFlight, result.get(0), "Этот перелёт должен пройти все фильтры");
    }
}
