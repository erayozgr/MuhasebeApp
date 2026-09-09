package com.eray.muhasebeapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.eray.muhasebeapp.data.network.ApiService
import com.eray.muhasebeapp.ui.MainStructure
import com.eray.muhasebeapp.ui.screens.GirisScreen
import com.eray.muhasebeapp.util.SessionManager

@Composable
fun App(
    guncelTarih: String,
    simdiMillis: Long
) {

    val kayitliOturumVar =
        remember {
            SessionManager.isSessionValid()
        }

    var oturumAcildiMi by remember {
        mutableStateOf(kayitliOturumVar)
    }

    var aktifKullaniciId by remember {

        mutableStateOf<Long?>(
            if (kayitliOturumVar) {

                val id =
                    SessionManager.getUserId()

                if (id > 0) {
                    id
                } else {
                    null
                }

            } else {

                null
            }
        )
    }

    var aktifKullaniciAdi by remember {

        mutableStateOf(
            if (kayitliOturumVar) {
                SessionManager.getAdSoyad()
            } else {
                ""
            }
        )
    }

    var aktifKullaniciEmail by remember {

        mutableStateOf(
            if (kayitliOturumVar) {
                SessionManager.getEmail()
            } else {
                ""
            }
        )
    }

    val apiService =
        remember {
            ApiService()
        }

    if (!oturumAcildiMi) {

        GirisScreen(

            onGirisBasarili = {
                    id,
                    adSoyad,
                    email ->

                aktifKullaniciId = id
                aktifKullaniciAdi = adSoyad
                aktifKullaniciEmail = email

                oturumAcildiMi = true
            }
        )

    } else {

        val kullaniciId =
            aktifKullaniciId

        if (kullaniciId != null) {

            MainStructure(
                apiService = apiService,

                kullaniciId = kullaniciId,

                kullaniciAdi =
                    aktifKullaniciAdi,

                kullaniciEmail =
                    aktifKullaniciEmail,

                onCikisYap = {

                    SessionManager.clearSession()

                    oturumAcildiMi = false

                    aktifKullaniciId = null
                    aktifKullaniciAdi = ""
                    aktifKullaniciEmail = ""
                },

                guncelTarih = guncelTarih,

                simdiMillis = simdiMillis
            )

        } else {
            SessionManager.clearSession()

            oturumAcildiMi = false
            aktifKullaniciAdi = ""
            aktifKullaniciEmail = ""
        }
    }
}