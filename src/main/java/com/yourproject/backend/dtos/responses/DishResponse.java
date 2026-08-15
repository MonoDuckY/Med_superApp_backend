package com.yourproject.backend.dtos.responses;

import com.yourproject.backend.models.Dish;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DishResponse {
    String id;
    String mealId;
    String dishName;
    Double quantity;
    String unit;
    Double totalCalories;
    Double totalProtein;
    Double totalCarbohydrates;
    Double totalFat;

    public static DishResponse from(Dish dish) {
        return DishResponse.builder().id(dish.getId()).mealId(dish.getMealId()).dishName(dish.getDishName())
                .quantity(dish.getQuantity()).unit(dish.getUnit()).totalCalories(dish.getTotalCalories())
                .totalProtein(dish.getTotalProtein()).totalCarbohydrates(dish.getTotalCarbohydrates())
                .totalFat(dish.getTotalFat()).build();
    }
}
