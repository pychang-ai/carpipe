/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

const val NEWPIPE_VERSION_SDK_COMPILE_MAJOR = 37
const val NEWPIPE_VERSION_SDK_COMPILE_MINOR = 0
const val NEWPIPE_VERSION_SDK_MIN = 23
const val NEWPIPE_VERSION_SDK_TARGET = 35

const val NEWPIPE_VERSION_CODE = 1015
const val NEWPIPE_VERSION_NAME = "0.29.1"

// CAI PP counts its own releases: the first number changes when the app gains or loses a
// whole feature, the second when an existing one is changed or fixed. The NewPipe version
// above is kept and shown alongside it, because almost all of the app is still their work
// and a fault report has to say which of the two it came from.
const val CAI_PP_VERSION_NAME = "1.0"

const val NEWPIPE_APPLICATION_ID_OLD = "org.schabi.newpipe"
const val NEWPIPE_APPLICATION_ID_NEW = "net.newpipe.app"
