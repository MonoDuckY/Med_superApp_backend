package com.yourproject.backend.repositories;

import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.yourproject.backend.models.Dish;

public interface DishRepository extends MongoRepository<Dish, String> {
    List<Dish> findAllByMealIdOrderByIdAsc(String mealId);
    List<Dish> findAllByMealIdIn(Collection<String> mealIds);
    void deleteAllByMealIdIn(Collection<String> mealIds);
}
