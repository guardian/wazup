package com.gu.wazup

import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}

object Date {
  private val simpleDateFormatter = DateTimeFormat.forPattern("yyyy-MMM-dd").withZoneUTC()

  def formatDate(date: DateTime): String = {
    simpleDateFormatter.print(date).toUpperCase
  }

  def today: DateTime = {
    DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay
  }
}
