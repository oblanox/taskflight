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
    void testDepartureEqualToNowIsValid() {
        LocalDateTime now = LocalDateTime.now();
        Flight flight = new Flight(List.of(new Segment(now, now.plusHours(1))));
        DepartureBeforeNowFilter filter = new DepartureBeforeNowFilter();

        assertTrue(filter.isValid(flight),"Вылет точно в текущий момент времени (now) должен считаться валидным");
    }
}
