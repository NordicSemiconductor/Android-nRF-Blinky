package no.nordicsemi.android.blinky.spec

sealed class BlinkyError: Exception()

class NotSupported: BlinkyError()
class ConnectionFailed: BlinkyError()
class LinkLoss: BlinkyError()
class Timeout: BlinkyError()