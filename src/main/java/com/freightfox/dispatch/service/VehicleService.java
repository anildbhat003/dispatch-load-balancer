package com.freightfox.dispatch.service;

import com.freightfox.dispatch.dto.VehicleDto;
import com.freightfox.dispatch.mapper.DispatchMapper;
import com.freightfox.dispatch.model.Vehicle;
import com.freightfox.dispatch.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    private final VehicleRepository repository;

    public VehicleService(VehicleRepository repository) {
        this.repository = repository;
    }

    /** Inserts new vehicles and updates existing ones (matched by vehicleId) in one transaction. */
    @Transactional
    public int upsertVehicles(List<VehicleDto> vehicles) {
        DuplicateIdValidator.ensureUnique(vehicles, VehicleDto::vehicleId, "vehicleId");

        List<String> ids = vehicles.stream().map(v -> v.vehicleId().trim()).toList();
        Map<String, Vehicle> existing = repository.findAllById(ids).stream()
                .collect(Collectors.toMap(Vehicle::getVehicleId, Function.identity()));

        List<Vehicle> toSave = new ArrayList<>(vehicles.size());
        for (VehicleDto dto : vehicles) {
            Vehicle entity = existing.get(dto.vehicleId().trim());
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
    public List<Vehicle> findAll() {
        return repository.findAll();
    }

    @Transactional
    public void deleteAll() {
        repository.deleteAllInBatch();
    }
}
