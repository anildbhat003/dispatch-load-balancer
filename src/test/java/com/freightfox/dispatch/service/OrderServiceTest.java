package com.freightfox.dispatch.service;

import com.freightfox.dispatch.dto.OrderDto;
import com.freightfox.dispatch.exception.InvalidRequestException;
import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Priority;
import com.freightfox.dispatch.repository.DeliveryOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private DeliveryOrderRepository repository;

    @InjectMocks
    private OrderService service;

    private static OrderDto dto(String id, double weight) {
        return new OrderDto(id, 12.97, 77.59, "MG Road", weight, Priority.HIGH);
    }

    @Test
    void rejectsDuplicateIdsInOneRequest() {
        List<OrderDto> orders = List.of(dto("ORD1", 1), dto("ORD2", 1), dto(" ORD1 ", 2));

        assertThatThrownBy(() -> service.upsertOrders(orders))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Duplicate orderId")
                .satisfies(ex -> assertThat(((InvalidRequestException) ex).getErrors())
                        .containsExactly("orderId 'ORD1' appears more than once"));
        verify(repository, never()).saveAll(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void insertsNewAndUpdatesExistingWithSingleLookup() {
        DeliveryOrder existing = new DeliveryOrder("ORD1", 0, 0, "old", 5, Priority.LOW);
        when(repository.findAllById(anyIterable())).thenReturn(List.of(existing));

        int saved = service.upsertOrders(List.of(dto("ORD1", 25), dto("ORD2", 10)));

        assertThat(saved).isEqualTo(2);
        ArgumentCaptor<List<DeliveryOrder>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<DeliveryOrder> savedOrders = captor.getValue();

        assertThat(savedOrders.get(0)).isSameAs(existing);
        assertThat(existing.getPackageWeight()).isEqualTo(25);
        assertThat(existing.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(existing.getAddress()).isEqualTo("MG Road");
        assertThat(savedOrders.get(1).getOrderId()).isEqualTo("ORD2");
        assertThat(savedOrders.get(1).isNew()).isTrue();
    }
}
