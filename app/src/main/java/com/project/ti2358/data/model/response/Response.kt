package com.project.ti2358.data.model.response

open class Response<T>(val trackingId: String, val status: String, val payload: T) {
}