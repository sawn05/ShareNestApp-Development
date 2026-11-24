package com.example.sharecare.repositories;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Map;

public class RequestRepositoryTest {

    @Test
    public void completionUpdates_movesReservedProductToCompleted() {
        Map<String, Object> updates =
                RequestRepository.completionUpdates(1L, 1L, 0L);

        assertEquals("COMPLETED", updates.get("status"));
        assertEquals(0L, updates.get("quantityAvailable"));
        assertEquals(0L, updates.get("quantityReserved"));
        assertEquals(1L, updates.get("quantityCompleted"));
    }

    @Test
    public void completionUpdates_acceptsNumericStringsAndMissingValues() {
        Map<String, Object> updates =
                RequestRepository.completionUpdates("3", null, "2");

        assertEquals(2L, updates.get("quantityAvailable"));
        assertEquals(0L, updates.get("quantityReserved"));
        assertEquals(3L, updates.get("quantityCompleted"));
    }

    @Test
    public void completionUpdates_neverGoesBelowZero() {
        Map<String, Object> updates =
                RequestRepository.completionUpdates(0L, 0L, 0L);

        assertEquals(0L, updates.get("quantityAvailable"));
        assertEquals(0L, updates.get("quantityReserved"));
        assertEquals(1L, updates.get("quantityCompleted"));
    }

    @Test
    public void toLong_readsCommaDecimalAndRejectsInvalidText() {
        assertEquals(21L, RequestRepository.toLong("21,9"));
        assertEquals(3L, RequestRepository.toLong("3"));
        assertEquals(0L, RequestRepository.toLong("abc"));
        assertEquals(0L, RequestRepository.toLong(null));
    }
}
