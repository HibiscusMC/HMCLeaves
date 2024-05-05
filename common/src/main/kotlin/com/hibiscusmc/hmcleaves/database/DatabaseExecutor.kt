package com.hibiscusmc.hmcleaves.database

interface DatabaseExecutor {

    fun executeRead(runnable: Runnable)

    fun executeWrite(runnable: Runnable)

}