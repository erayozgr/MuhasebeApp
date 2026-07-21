package com.eray.muhasebeapp

import com.eray.muhasebeapp.database.Alis
import com.eray.muhasebeapp.database.Masraf
import com.eray.muhasebeapp.database.Satis
import com.eray.muhasebeapp.database.StokHareketi

/**
 * csvRaporuOlustur yerine kullanılacak yeni fonksiyon.
 * Aynı parametreleri alır, ama düz metin CSV yerine gerçek bir Excel dosyası
 * (SpreadsheetML / .xls) üretir: başlıklar renkli, para birimi hücreleri
 * biçimli, her tablonun altında toplam satırı ve donmuş üst satır var.
 *
 * SpreadsheetML tercih edildi çünkü tamamen düz XML metni olarak commonMain
 * içinde üretilebiliyor; gerçek .xlsx (zip) üretmek KMP'de platforma özel
 * zip kütüphanesi gerektirir. Excel, Numbers ve Google Sheets bu formatı
 * "gerçek" bir çalışma kitabı olarak açar.
 */
fun excelRaporuOlustur(
    satislar: List<Satis>,
    alislar: List<Alis>,
    masraflar: List<Masraf>,
    stokHareketleri: List<StokHareketi> = emptyList()
): String {
    val toplamSatis = satislar.sumOf { it.toplamTutar }
    val toplamAlis = alislar.sumOf { it.toplamTutar }
    val odenenMasraf = masraflar.filter { it.odendiMi == 1L }.sumOf { it.tutar }
    val bekleyenMasraf = masraflar.filter { it.odendiMi != 1L }.sumOf { it.tutar }
    val toplamMasraf = odenenMasraf + bekleyenMasraf
    val brutKar = toplamSatis - toplamAlis
    val netKar = toplamSatis - toplamAlis - odenenMasraf
    val ortalamaSatis = if (satislar.isNotEmpty()) toplamSatis / satislar.size else 0.0
    val ortalamaAlis = if (alislar.isNotEmpty()) toplamAlis / alislar.size else 0.0
    val toplamStokTutari = stokHareketleri.sumOf { it.birimFiyat * it.miktar }

    val sb = StringBuilder()
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    sb.append("<?mso-application progid=\"Excel.Sheet\"?>\n")
    sb.append(
        "<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" " +
                "xmlns:o=\"urn:schemas-microsoft-com:office:office\" " +
                "xmlns:x=\"urn:schemas-microsoft-com:office:excel\" " +
                "xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n"
    )
    sb.append(STYLES)

    // ---- ÖZET ----
    sb.append("<Worksheet ss:Name=\"Ozet\">\n<Table ss:DefaultColumnWidth=\"140\">\n")
    sb.append("<Column ss:Width=\"240\"/><Column ss:Width=\"150\"/>\n")
    sb.append(row("<Cell ss:StyleID=\"Baslik\" ss:MergeAcross=\"1\"><Data ss:Type=\"String\">Muhasebe Raporu - Ozet</Data></Cell>", height = 28))
    sb.append(row(""))
    sb.append(labelValueRow("Toplam Satis", toplamSatis))
    sb.append(labelValueRow("Toplam Alis", toplamAlis))
    sb.append(labelValueRow("Odenen Masraf", odenenMasraf))
    sb.append(labelValueRow("Bekleyen Masraf", bekleyenMasraf))
    sb.append(labelValueRow("Toplam Masraf", toplamMasraf))
    sb.append(labelValueRow("Brut Kar (Satis - Alis)", brutKar))
    sb.append(row(""))
    sb.append(
        row(
            "<Cell ss:StyleID=\"ToplamEtiket\"><Data ss:Type=\"String\">Net Kar / Zarar (Nakit)</Data></Cell>" +
                    "<Cell ss:StyleID=\"${if (netKar >= 0) "ParaPozitif" else "ParaNegatif"}\">" +
                    "<Data ss:Type=\"Number\">${fmtNum(netKar)}</Data></Cell>"
        )
    )
    sb.append(row(""))
    sb.append(labelValueRow("Satis Adedi", satislar.size.toDouble(), isCount = true))
    sb.append(labelValueRow("Alis Adedi", alislar.size.toDouble(), isCount = true))
    sb.append(labelValueRow("Masraf Adedi", masraflar.size.toDouble(), isCount = true))
    sb.append(labelValueRow("Stok Hareketi Adedi", stokHareketleri.size.toDouble(), isCount = true))
    sb.append(labelValueRow("Ortalama Satis Tutari", ortalamaSatis))
    sb.append(labelValueRow("Ortalama Alis Tutari", ortalamaAlis))
    sb.append(labelValueRow("Stok Hareketleri Toplam Tutar", toplamStokTutari))
    sb.append("</Table>\n</Worksheet>\n")

    // ---- SATIŞLAR ----
    sb.append("<Worksheet ss:Name=\"Satislar\">\n<Table>\n")
    sb.append("<Column ss:Width=\"120\"/><Column ss:Width=\"200\"/><Column ss:Width=\"120\"/><Column ss:Width=\"120\"/>\n")
    sb.append(headerRow("Tarih", "Musteri", "Odeme Turu", "Tutar"))
    satislar.forEach { s ->
        sb.append(
            row(
                textCell(formatTarih(s.tarih)) +
                        textCell(s.musteriAdi) +
                        textCell(s.odemeTuru) +
                        moneyCell(s.toplamTutar)
            )
        )
    }
    sb.append(totalRow("TOPLAM", toplamSatis, mergeAcross = 2))
    sb.append(freezeHeader())
    sb.append("</Table>\n</Worksheet>\n")

    // ---- ALIŞLAR ----
    sb.append("<Worksheet ss:Name=\"Alislar\">\n<Table>\n")
    sb.append("<Column ss:Width=\"120\"/><Column ss:Width=\"200\"/><Column ss:Width=\"120\"/><Column ss:Width=\"120\"/>\n")
    sb.append(headerRow("Tarih", "Tedarikci", "Odeme Turu", "Tutar"))
    alislar.forEach { a ->
        sb.append(
            row(
                textCell(formatTarih(a.tarih)) +
                        textCell(a.tedarikciAdi) +
                        textCell(a.odemeTuru) +
                        moneyCell(a.toplamTutar)
            )
        )
    }
    sb.append(totalRow("TOPLAM", toplamAlis, mergeAcross = 2))
    sb.append(freezeHeader())
    sb.append("</Table>\n</Worksheet>\n")

    // ---- MASRAFLAR ----
    sb.append("<Worksheet ss:Name=\"Masraflar\">\n<Table>\n")
    sb.append("<Column ss:Width=\"120\"/><Column ss:Width=\"140\"/><Column ss:Width=\"220\"/><Column ss:Width=\"100\"/><Column ss:Width=\"120\"/><Column ss:Width=\"120\"/>\n")
    sb.append(headerRow("Tarih", "Kategori", "Aciklama", "Durum", "Son Odeme Tarihi", "Tutar"))
    masraflar.forEach { m ->
        sb.append(
            row(
                textCell(formatTarih(m.tarih)) +
                        textCell(m.kategori) +
                        textCell(m.aciklama) +
                        textCell(if (m.odendiMi == 1L) "Odendi" else "Bekliyor") +
                        textCell(if (m.sonOdemeTarihi.isNotBlank()) formatTarih(m.sonOdemeTarihi) else "-") +
                        moneyCell(m.tutar)
            )
        )
    }
    sb.append(totalRow("TOPLAM", toplamMasraf, mergeAcross = 4))
    sb.append(freezeHeader())
    sb.append("</Table>\n</Worksheet>\n")

    // ---- STOK HAREKETLERİ ----
    sb.append("<Worksheet ss:Name=\"Stok Hareketleri\">\n<Table>\n")
    sb.append("<Column ss:Width=\"120\"/><Column ss:Width=\"180\"/><Column ss:Width=\"110\"/><Column ss:Width=\"90\"/><Column ss:Width=\"110\"/><Column ss:Width=\"200\"/><Column ss:Width=\"120\"/>\n")
    sb.append(headerRow("Tarih", "Urun", "Hareket Turu", "Miktar", "Birim Fiyat", "Aciklama", "Tutar"))
    stokHareketleri.forEach { h ->
        sb.append(
            row(
                textCell(formatTarih(h.tarih)) +
                        textCell(h.urunAdi) +
                        textCell(h.hareketTuru) +
                        numberCell(h.miktar.toDouble()) +
                        moneyCell(h.birimFiyat) +
                        textCell(h.aciklama) +
                        moneyCell(h.birimFiyat * h.miktar)
            )
        )
    }
    sb.append(totalRow("TOPLAM", toplamStokTutari, mergeAcross = 5))
    sb.append(freezeHeader())
    sb.append("</Table>\n</Worksheet>\n")

    sb.append("</Workbook>")
    return sb.toString()
}

// ---------------- yardımcılar ----------------

private fun esc(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&apos;")

private fun fmtNum(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

private fun row(cellsXml: String, height: Int? = null): String {
    val h = if (height != null) " ss:Height=\"$height\"" else ""
    return "<Row$h>$cellsXml</Row>\n"
}

private fun headerRow(vararg titles: String): String {
    val cells = titles.joinToString("") {
        "<Cell ss:StyleID=\"TabloBaslik\"><Data ss:Type=\"String\">${esc(it)}</Data></Cell>"
    }
    return row(cells, height = 22)
}

private fun textCell(v: String) =
    "<Cell ss:StyleID=\"Hucre\"><Data ss:Type=\"String\">${esc(v)}</Data></Cell>"

private fun moneyCell(v: Double) =
    "<Cell ss:StyleID=\"Para\"><Data ss:Type=\"Number\">${fmtNum(v)}</Data></Cell>"

private fun numberCell(v: Double) =
    "<Cell ss:StyleID=\"Hucre\"><Data ss:Type=\"Number\">${fmtNum(v)}</Data></Cell>"

private fun labelValueRow(label: String, value: Double, isCount: Boolean = false): String {
    val style = if (isCount) "Hucre" else "Para"
    return "<Row><Cell ss:StyleID=\"Etiket\"><Data ss:Type=\"String\">${esc(label)}</Data></Cell>" +
            "<Cell ss:StyleID=\"$style\"><Data ss:Type=\"Number\">${fmtNum(value)}</Data></Cell></Row>\n"
}

private fun totalRow(label: String, value: Double, mergeAcross: Int): String {
    return "<Row><Cell ss:StyleID=\"ToplamEtiket\" ss:MergeAcross=\"$mergeAcross\">" +
            "<Data ss:Type=\"String\">${esc(label)}</Data></Cell>" +
            "<Cell ss:StyleID=\"ToplamPara\"><Data ss:Type=\"Number\">${fmtNum(value)}</Data></Cell></Row>\n"
}

private fun freezeHeader(): String =
    "<WorksheetOptions xmlns=\"urn:schemas-microsoft-com:office:excel\">" +
            "<FreezePanes/><FrozenNoSplit/><SplitHorizontal>1</SplitHorizontal>" +
            "<TopRowBottomPane>1</TopRowBottomPane><ActivePane>2</ActivePane>" +
            "</WorksheetOptions>\n"

private const val STYLES = """
<Styles>
    <Style ss:ID="Default" ss:Name="Normal">
        <Font ss:FontName="Calibri" ss:Size="11"/>
    </Style>
    <Style ss:ID="Baslik">
        <Font ss:FontName="Calibri" ss:Size="16" ss:Bold="1" ss:Color="#FFFFFF"/>
        <Interior ss:Color="#1F4E78" ss:Pattern="Solid"/>
        <Alignment ss:Horizontal="Left" ss:Vertical="Center"/>
    </Style>
    <Style ss:ID="TabloBaslik">
        <Font ss:FontName="Calibri" ss:Size="11" ss:Bold="1" ss:Color="#FFFFFF"/>
        <Interior ss:Color="#2E75B6" ss:Pattern="Solid"/>
        <Alignment ss:Horizontal="Center" ss:Vertical="Center" ss:WrapText="1"/>
        <Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1"/></Borders>
    </Style>
    <Style ss:ID="Etiket">
        <Font ss:FontName="Calibri" ss:Size="11" ss:Bold="1" ss:Color="#333333"/>
        <Interior ss:Color="#F2F2F2" ss:Pattern="Solid"/>
        <Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#D9D9D9"/></Borders>
    </Style>
    <Style ss:ID="Hucre">
        <Font ss:FontName="Calibri" ss:Size="11"/>
        <Alignment ss:Vertical="Top" ss:WrapText="1"/>
        <Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E7E7E7"/></Borders>
    </Style>
    <Style ss:ID="Para">
        <Font ss:FontName="Calibri" ss:Size="11"/>
        <NumberFormat ss:Format="#,##0.00 &quot;TL&quot;"/>
        <Alignment ss:Horizontal="Right" ss:Vertical="Top"/>
        <Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E7E7E7"/></Borders>
    </Style>
    <Style ss:ID="ParaPozitif">
        <Font ss:FontName="Calibri" ss:Size="11" ss:Bold="1" ss:Color="#217346"/>
        <NumberFormat ss:Format="#,##0.00 &quot;TL&quot;"/>
        <Interior ss:Color="#E2EFDA" ss:Pattern="Solid"/>
        <Alignment ss:Horizontal="Right" ss:Vertical="Center"/>
    </Style>
    <Style ss:ID="ParaNegatif">
        <Font ss:FontName="Calibri" ss:Size="11" ss:Bold="1" ss:Color="#C00000"/>
        <NumberFormat ss:Format="#,##0.00 &quot;TL&quot;"/>
        <Interior ss:Color="#FCE4E4" ss:Pattern="Solid"/>
        <Alignment ss:Horizontal="Right" ss:Vertical="Center"/>
    </Style>
    <Style ss:ID="ToplamEtiket">
        <Font ss:FontName="Calibri" ss:Size="11" ss:Bold="1"/>
        <Interior ss:Color="#DDEBF7" ss:Pattern="Solid"/>
        <Borders><Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="2" ss:Color="#2E75B6"/></Borders>
    </Style>
    <Style ss:ID="ToplamPara">
        <Font ss:FontName="Calibri" ss:Size="11" ss:Bold="1"/>
        <NumberFormat ss:Format="#,##0.00 &quot;TL&quot;"/>
        <Interior ss:Color="#DDEBF7" ss:Pattern="Solid"/>
        <Alignment ss:Horizontal="Right"/>
        <Borders><Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="2" ss:Color="#2E75B6"/></Borders>
    </Style>
</Styles>
"""