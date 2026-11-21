package com.freightfox.dispatch.service;

import com.freightfox.dispatch.dto.OrderDto;
import com.freightfox.dispatch.mapper.DispatchMapper;
import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.repository.DeliveryOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final DeliveryOrderRepository repository;

    public OrderService(DeliveryOrderRepository repository) {
        this.repository = repository;
    }

    /**
     * Inserts new orders and updates existing ones (matched by orderId) in one transaction.
     * Uses a single {@code SELECT ... WHERE id IN (...)} to find existing rows, then batched writes.
     *
     * @return number of orders saved
     */
    @Transactional
    public int upsertOrders(List<OrderDto> orders) {
        DuplicateIdValidator.ensureUnique(orders, OrderDto::orderId, "orderId");

        List<String> ids = orders.stream().map(o -> o.orderId().trim()).toList();
        Map<String, DeliveryOrder> existing = repository.findAllById(ids).stream()
                .collect(Collectors.toMap(DeliveryOrder::getOrderId, Function.identity()));

        List<DeliveryOrder> toSave = new ArrayList<>(orders.size());
        for (OrderDto dto : orders) {
            DeliveryOrder entity = existing.get(dto.orderId().trim());
            if (entity == null) {
                toSave.add(DispatchMapper.toEntity(dto));
            } else {
                DispatchMapper.updateEntity(entity, dto);
                toSave.add(entity);
            }
        }
        repository.saveAll(toSave);
        return toSave.size();
    }

    @Transactional(readOnly = true)
    public List<DeliveryOrder> findAll() {
        return repository.findAll();
    }

    @Transactional
    public void deleteAll() {
        repository.deleteAllInBatch();
    }
}
