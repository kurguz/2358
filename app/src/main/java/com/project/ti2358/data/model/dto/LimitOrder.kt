package com.project.ti2358.data.model.dto

data class LimitOrder(
    val orderId: String,
    val operation: OperationType,
    val status: OrderStatus,
    val rejectReason: String,
    val message: String,
    val requestedLots: Int,
    val executedLots: Int,
    val commission: MoneyAmount?,

    var figi: String
)