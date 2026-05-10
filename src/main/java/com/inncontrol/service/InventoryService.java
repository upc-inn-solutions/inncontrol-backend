package com.inncontrol.service;

import com.inncontrol.model.InventoryItem;
import com.inncontrol.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public List<InventoryItem> getAllItems() {
        return inventoryRepository.findAll();
    }

    public InventoryItem createItem(InventoryItem item) {
        return inventoryRepository.save(item);
    }

    public InventoryItem updateItem(Long id, InventoryItem itemDetails) {
        InventoryItem item = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Artículo no encontrado"));
        
        item.setName(itemDetails.getName());
        item.setCategory(itemDetails.getCategory());
        item.setCurrentQuantity(itemDetails.getCurrentQuantity());
        item.setMinQuantity(itemDetails.getMinQuantity());
        
        return inventoryRepository.save(item);
    }

    public InventoryItem updateQuantity(Long id, Integer quantityChange) {
        InventoryItem item = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Artículo no encontrado"));
        item.setCurrentQuantity(Math.max(0, item.getCurrentQuantity() + quantityChange));
        return inventoryRepository.save(item);
    }

    public void deleteItem(Long id) {
        inventoryRepository.deleteById(id);
    }
}
