package com.example.rutalogcourier.data.repository

import androidx.room.withTransaction
import com.example.rutalogcourier.data.local.AppDatabase
import com.example.rutalogcourier.data.local.SesionSegura
import com.example.rutalogcourier.data.local.entities.UsuarioEntity
import com.example.rutalogcourier.data.remote.ResultadoApi
import com.example.rutalogcourier.data.remote.api.ApiService
import com.example.rutalogcourier.data.remote.dto.LoginRequest
import com.example.rutalogcourier.data.remote.dto.LoginResponse
import com.example.rutalogcourier.data.remote.dto.SocialRequest
import com.example.rutalogcourier.data.remote.llamarApi
import com.example.rutalogcourier.model.Usuario

sealed interface ResultadoLogin {
    data class Exito(val usuario: Usuario) : ResultadoLogin
    data class Error(val mensaje: String) : ResultadoLogin
}

/**
 * Autenticación mediante la API REST. Solo entran cuentas con rol "courier".
 * - El token se guarda cifrado (SesionSegura) y el perfil en la tabla usuarios.
 * - Quien ya inició sesión entra aunque no haya señal: en la calle es lo normal.
 */
class AuthRepository(
    private val api: ApiService,
    private val db: AppDatabase,
    private val sesion: SesionSegura
) {

    suspend fun usuarioActual(): Usuario? =
        if (sesion.token == null) null else db.usuarioDao().actual()?.let { Usuario(it.id, it.nombre, it.correo, it.rol) }

    suspend fun login(correo: String, clave: String): ResultadoLogin =
        abrirSesion(llamarApi { api.login(LoginRequest(correo, clave)) })

    /** Google (vía Firebase): solo entra si la empresa ya registró ese correo. */
    suspend fun loginConGoogle(idTokenFirebase: String): ResultadoLogin =
        abrirSesion(llamarApi { api.loginSocial(SocialRequest(idTokenFirebase, "courier")) })

    private suspend fun abrirSesion(resultado: ResultadoApi<LoginResponse>): ResultadoLogin =
        when (resultado) {
            is ResultadoApi.Exito -> {
                val perfil = resultado.valor.usuario
                if (perfil.rol != ROL_COURIER) {
                    ResultadoLogin.Error("Esta cuenta no es de courier. Usa la App Cliente o la App Admin.")
                } else {
                    db.withTransaction {
                        val anterior = db.usuarioDao().actual()
                        // Si entra otro courier en este teléfono, no hereda la ruta ni los registros del anterior.
                        if (anterior != null && anterior.id != perfil.id) {
                            db.paradaDao().borrarTodo()
                            db.paradaDao().borrarResumen()
                            db.operacionPendienteDao().borrarTodo()
                            db.operacionPendienteDao().borrarMetadata()
                        }
                        db.usuarioDao().borrarTodo()
                        db.usuarioDao().guardar(UsuarioEntity(perfil.id, perfil.nombre, perfil.correo, perfil.rol))
                    }
                    sesion.token = resultado.valor.token
                    ResultadoLogin.Exito(Usuario(perfil.id, perfil.nombre, perfil.correo, perfil.rol))
                }
            }

            is ResultadoApi.Rechazo -> ResultadoLogin.Error(resultado.mensaje)

            is ResultadoApi.Fallo -> ResultadoLogin.Error(
                if (resultado.sinConexion) "Sin conexión. Necesitas internet para iniciar sesión." else resultado.mensaje
            )
        }

    /** Cierra la sesión. La ruta y los registros sin enviar se conservan por si el mismo courier vuelve a entrar. */
    fun cerrarSesion() {
        sesion.cerrar()
    }

    private companion object {
        const val ROL_COURIER = "courier"
    }
}
