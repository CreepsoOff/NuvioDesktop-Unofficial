package com.nuvio.app.features.watchprogress

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

actual object CurrentDateProvider {
    actual fun todayIsoDate(): String {
        val formatter = NSDateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.stringFromDate(NSDate())
    }
}

