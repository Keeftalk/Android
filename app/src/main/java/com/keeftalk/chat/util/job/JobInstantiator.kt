package com.keeftalk.chat.util.job

interface JobInstantiator {
    fun create(spec: JobSpec): Job
}
