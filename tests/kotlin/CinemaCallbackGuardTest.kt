import com.example.piliplus.CinemaCallbackGuard

fun main() {
    val cases = mutableListOf<String>()
    val guard = CinemaCallbackGuard(41L)
    val catalog = guard.begin()
    check(guard.accepts(catalog, 41L))
    cases += "current_catalog_callback_accepted"
    val quality = guard.begin()
    check(!guard.accepts(catalog, 41L) && guard.accepts(quality, 41L))
    cases += "new_request_invalidates_old_catalog"
    check(!guard.accepts(quality, 42L))
    cases += "reopened_session_rejects_old_quality"
    check(!guard.accepts(quality, 41L, finishing = true))
    cases += "exit_requested_before_destroy_rejects_success_and_error"
    check(!guard.accepts(quality, 41L, destroyed = true))
    cases += "destroyed_activity_flag_rejects_callback_before_guard_close"
    var mediaWrites = 0
    var playerLoads = 0
    fun lateSuccess() {
        if (!guard.accepts(quality, 41L)) return
        ++mediaWrites
        ++playerLoads
    }
    guard.close()
    lateSuccess()
    check(mediaWrites == 0 && playerLoads == 0)
    cases += "destroyed_preview_rejects_success_without_mutation"
    check(!guard.accepts(quality, 41L))
    cases += "destroyed_preview_rejects_error_callback"
    val postDestroy = guard.begin()
    check(!guard.accepts(postDestroy, 41L))
    cases += "new_epoch_cannot_reactivate_closed_preview"
    val reopened = CinemaCallbackGuard(42L)
    val fresh = reopened.begin()
    check(reopened.accepts(fresh, 42L))
    check(!guard.accepts(quality, 42L))
    cases += "new_preview_can_start_while_old_callback_stays_invalid"
    println("{\"passed\":${cases.size},\"cases\":\"${cases.joinToString(",")}\",\"coverage\":\"Production Kotlin callback guard only; no Android Activity or player runtime\"}")
}
