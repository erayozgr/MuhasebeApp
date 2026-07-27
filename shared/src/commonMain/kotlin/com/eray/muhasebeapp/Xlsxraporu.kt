package com.eray.muhasebeapp

import com.eray.muhasebeapp.database.Alis
import com.eray.muhasebeapp.database.Masraf
import com.eray.muhasebeapp.database.Satis
import com.eray.muhasebeapp.database.StokHareketi
import com.eray.muhasebeapp.database.SatisKalemi
import com.eray.muhasebeapp.database.AlisKalemi
import com.eray.muhasebeapp.database.Tahsilat // 🌟 Eklendi

fun excelXlsxOlustur(
    satislar: List<Satis>,
    alislar: List<Alis>,
    masraflar: List<Masraf>,
    stokHareketleri: List<StokHareketi> = emptyList(),
    tahsilatlar: List<Tahsilat> = emptyList(),
    raporTuru: String = "Genel Rapor",
    satisKalemleriGetir: (Long) -> List<SatisKalemi> = { emptyList() },
    alisKalemleriGetir: (Long) -> List<AlisKalemi> = { emptyList() },
    baslangicMs: Long? = null,
    bitisMs: Long? = null
): ByteArray  {
    val toplamSatis = satislar.sumOf { it.toplamTutar }
    val toplamAlis = alislar.sumOf { it.toplamTutar }
    val toplamMasraf = masraflar.sumOf { it.tutar } // 🌟 odendiMi kaldırıldığı için direkt toplam alındı
    val brutKar = toplamSatis - toplamAlis
    val netKar = toplamSatis - toplamAlis - toplamMasraf // 🌟 Net kar toplam masrafa göre güncellendi
    val ortalamaSatis = if (satislar.isNotEmpty()) toplamSatis / satislar.size else 0.0
    val ortalamaAlis = if (alislar.isNotEmpty()) toplamAlis / alislar.size else 0.0
    val toplamStokTutari = stokHareketleri.sumOf { it.birimFiyat * it.miktar }
    val toplamTahsilat = tahsilatlar.sumOf { it.tutar }

    fun ozetSheet(): String = SheetBuilder().apply {
        row(Cell.Text("Muhasebe Raporu - Ozet", S_TITLE))
        blank()
        row(Cell.Text("Toplam Satis", S_LABEL), Cell.Num(toplamSatis, S_CURRENCY))
        row(Cell.Text("Toplam Alis", S_LABEL), Cell.Num(toplamAlis, S_CURRENCY))
        row(Cell.Text("Toplam Tahsilat (Nakit Girişi)", S_LABEL), Cell.Num(toplamTahsilat, S_CURRENCY))
        row(Cell.Text("Toplam Masraf", S_LABEL), Cell.Num(toplamMasraf, S_CURRENCY)) // 🌟 Ödenen/bekleyen satırları temizlendi
        row(Cell.Text("Brut Kar (Satis - Alis)", S_LABEL), Cell.Num(brutKar, S_CURRENCY))
        blank()
        row(Cell.Text("Net Kar / Zarar (Nakit)", S_TOTAL_LABEL), Cell.Num(netKar, S_TOTAL_CURRENCY))
        blank()
        row(Cell.Text("Satis Adedi", S_LABEL), Cell.Num(satislar.size.toDouble()))
        row(Cell.Text("Alis Adedi", S_LABEL), Cell.Num(alislar.size.toDouble()))
        row(Cell.Text("Tahsilat Adedi", S_LABEL), Cell.Num(tahsilatlar.size.toDouble()))
        row(Cell.Text("Masraf Adedi", S_LABEL), Cell.Num(masraflar.size.toDouble()))
        row(Cell.Text("Stok Hareketi Adedi", S_LABEL), Cell.Num(stokHareketleri.size.toDouble()))
        row(Cell.Text("Ortalama Satis Tutari", S_LABEL), Cell.Num(ortalamaSatis, S_CURRENCY))
        row(Cell.Text("Ortalama Alis Tutari", S_LABEL), Cell.Num(ortalamaAlis, S_CURRENCY))
        row(Cell.Text("Stok Hareketleri Toplam Tutar", S_LABEL), Cell.Num(toplamStokTutari, S_CURRENCY))
    }.xml()

    fun satislarSheetDetayli(): String = SheetBuilder().apply {
        row(
            Cell.Text("Tarih", S_HEADER), Cell.Text("Musteri", S_HEADER),
            Cell.Text("Urun", S_HEADER), Cell.Text("Adet", S_HEADER),
            Cell.Text("Birim Fiyat", S_HEADER), Cell.Text("Satis Tutari", S_HEADER)
        )
        satislar.forEach { s ->
            val kalemler = satisKalemleriGetir(s.id)
            if (kalemler.isEmpty()) {
                row(
                    Cell.Text(formatTarih(s.tarih)), Cell.Text(s.musteriAdi),
                    Cell.Text("-"), Cell.Text("-"), Cell.Text("-"),
                    Cell.Num(s.toplamTutar, S_CURRENCY)
                )
            } else {
                kalemler.forEach { k ->
                    row(
                        Cell.Text(formatTarih(s.tarih)), Cell.Text(s.musteriAdi),
                        Cell.Text(k.urunAdi), Cell.Num(k.adet.toDouble()),
                        Cell.Num(k.birimFiyat, S_CURRENCY), Cell.Num(k.toplam, S_CURRENCY)
                    )
                }
            }
        }
        row(
            Cell.Text("TOPLAM", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL),
            Cell.Text("", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL),
            Cell.Num(toplamSatis, S_TOTAL_CURRENCY)
        )
    }.xml()

    fun alislarSheetDetayli(): String = SheetBuilder().apply {
        row(
            Cell.Text("Tarih", S_HEADER), Cell.Text("Tedarikci", S_HEADER),
            Cell.Text("Urun", S_HEADER), Cell.Text("Adet", S_HEADER),
            Cell.Text("Birim Fiyat", S_HEADER), Cell.Text("Alis Tutari", S_HEADER)
        )
        alislar.forEach { a ->
            val kalemler = alisKalemleriGetir(a.id)
            if (kalemler.isEmpty()) {
                row(
                    Cell.Text(formatTarih(a.tarih)), Cell.Text(a.tedarikciAdi),
                    Cell.Text("-"), Cell.Text("-"), Cell.Text("-"),
                    Cell.Num(a.toplamTutar, S_CURRENCY)
                )
            } else {
                kalemler.forEach { k ->
                    row(
                        Cell.Text(formatTarih(a.tarih)), Cell.Text(a.tedarikciAdi),
                        Cell.Text(k.urunAdi), Cell.Num(k.adet.toDouble()),
                        Cell.Num(k.birimFiyat, S_CURRENCY), Cell.Num(k.toplam, S_CURRENCY)
                    )
                }
            }
        }
        row(
            Cell.Text("TOPLAM", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL),
            Cell.Text("", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL),
            Cell.Num(toplamAlis, S_TOTAL_CURRENCY)
        )
    }.xml()

    fun masraflarSheet(): String = SheetBuilder().apply {
        // 🌟 Durum ve Son Ödeme Tarihi sütunları kaldırıldı, şemaya uygun 4 sütuna düşürüldü
        row(
            Cell.Text("Tarih", S_HEADER), Cell.Text("Kategori", S_HEADER),
            Cell.Text("Aciklama", S_HEADER), Cell.Text("Tutar", S_HEADER)
        )
        masraflar.forEach { m ->
            row(
                Cell.Text(formatTarih(m.tarih)),
                Cell.Text(m.kategori),
                Cell.Text(m.aciklama),
                Cell.Num(m.tutar, S_CURRENCY)
            )
        }
        row(
            Cell.Text("TOPLAM", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL),
            Cell.Text("", S_TOTAL_LABEL), Cell.Num(toplamMasraf, S_TOTAL_CURRENCY)
        )
    }.xml()

    fun stokSheet(): String = SheetBuilder().apply {
        row(
            Cell.Text("Tarih", S_HEADER), Cell.Text("Urun", S_HEADER), Cell.Text("Hareket Turu", S_HEADER),
            Cell.Text("Miktar", S_HEADER), Cell.Text("Birim Fiyat", S_HEADER), Cell.Text("Aciklama", S_HEADER), Cell.Text("Tutar", S_HEADER)
        )
        stokHareketleri.forEach { h ->
            row(
                Cell.Text(formatTarih(h.tarih)),
                Cell.Text(h.urunAdi),
                Cell.Text(h.hareketTuru),
                Cell.Num(h.miktar.toDouble()),
                Cell.Num(h.birimFiyat, S_CURRENCY),
                Cell.Text(h.aciklama),
                Cell.Num(h.birimFiyat * h.miktar, S_CURRENCY)
            )
        }
        row(
            Cell.Text("TOPLAM", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL),
            Cell.Text("", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL),
            Cell.Num(toplamStokTutari, S_TOTAL_CURRENCY)
        )
    }.xml()

    fun tahsilatlarSheet(): String = SheetBuilder().apply {
        row(
            Cell.Text("Tarih", S_HEADER), Cell.Text("Musteri", S_HEADER), Cell.Text("Tahsil Edilen Tutar", S_HEADER)
        )
        tahsilatlar.forEach { t ->
            row(
                Cell.Text(formatTarih(t.tarih)),
                Cell.Text(t.musteriAdi),
                Cell.Num(t.tutar, S_CURRENCY)
            )
        }
        row(
            Cell.Text("TOPLAM", S_TOTAL_LABEL), Cell.Text("", S_TOTAL_LABEL),
            Cell.Num(toplamTahsilat, S_TOTAL_CURRENCY)
        )
    }.xml()

    val sayfalar: List<Pair<String, String>> = when (raporTuru) {
        "Satış Raporu" -> listOf("Satislar" to satislarSheetDetayli())
        "Alış Raporu" -> listOf("Alislar" to alislarSheetDetayli())
        "Masraf Raporu" -> listOf("Masraflar" to masraflarSheet())
        "Stok Hareketi Raporu" -> listOf("Stok Hareketleri" to stokSheet())
        "Tahsilat Raporu" -> listOf("Tahsilatlar" to tahsilatlarSheet())
        else -> listOf(
            "Ozet" to ozetSheet(),
            "Satislar" to satislarSheetDetayli(),
            "Alislar" to alislarSheetDetayli(),
            "Masraflar" to masraflarSheet(),
            "Stok Hareketleri" to stokSheet(),
            "Tahsilatlar" to tahsilatlarSheet()
        )
    }

    return buildXlsx(sayfalar)
}

// ... Geri kalan private build fonksiyonları ve sınıflar aynen korunmuştur ...

private fun buildXlsx(sayfalar: List<Pair<String, String>>): ByteArray {
    val sheetEntries = sayfalar.mapIndexed { index, (_, dataXml) ->
        ZipEntryData("xl/worksheets/sheet${index + 1}.xml", worksheetXml(dataXml).encodeToByteArray())
    }

    val contentTypesOverrides = sayfalar.indices.joinToString("") { i ->
        "<Override PartName=\"/xl/worksheets/sheet${i + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
    }
    val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
$contentTypesOverrides
</Types>"""

    val sheetsXml = sayfalar.mapIndexed { index, (name, _) ->
        "<sheet name=\"${xmlEsc(name)}\" sheetId=\"${index + 1}\" r:id=\"rId${index + 1}\"/>"
    }.joinToString("")
    val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets>$sheetsXml</sheets>
</workbook>"""

    val worksheetRels = sayfalar.indices.joinToString("") { i ->
        "<Relationship Id=\"rId${i + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet${i + 1}.xml\"/>"
    }
    val stylesRelId = "rId${sayfalar.size + 1}"
    val workbookRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$worksheetRels
<Relationship Id="$stylesRelId" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    val entries = listOf(
        ZipEntryData("[Content_Types].xml", contentTypesXml.encodeToByteArray()),
        ZipEntryData("_rels/.rels", RELS_XML.encodeToByteArray()),
        ZipEntryData("xl/workbook.xml", workbookXml.encodeToByteArray()),
        ZipEntryData("xl/_rels/workbook.xml.rels", workbookRelsXml.encodeToByteArray()),
        ZipEntryData("xl/styles.xml", STYLES_XML.encodeToByteArray())
    ) + sheetEntries

    return buildZip(entries)
}

private const val S_DEFAULT = 0
private const val S_TITLE = 1
private const val S_HEADER = 2
private const val S_CURRENCY = 3
private const val S_LABEL = 4
private const val S_TOTAL_LABEL = 5
private const val S_TOTAL_CURRENCY = 6

private sealed class Cell {
    data class Text(val value: String, val style: Int = S_DEFAULT) : Cell()
    data class Num(val value: Double, val style: Int = S_DEFAULT) : Cell()
}

private class SheetBuilder {
    private val sb = StringBuilder()
    private var rowNum = 0

    fun row(vararg cells: Cell) {
        rowNum++
        sb.append("<row r=\"$rowNum\">")
        cells.forEachIndexed { i, cell ->
            val ref = colLetter(i) + rowNum
            when (cell) {
                is Cell.Text -> sb.append("<c r=\"$ref\" t=\"inlineStr\" s=\"${cell.style}\"><is><t>${xmlEsc(cell.value)}</t></is></c>")
                is Cell.Num -> sb.append("<c r=\"$ref\" s=\"${cell.style}\"><v>${fmtNum(cell.value)}</v></c>")
            }
        }
        sb.append("</row>")
    }

    fun blank() { rowNum++; sb.append("<row r=\"$rowNum\"/>") }
    fun xml(): String = "<sheetData>$sb</sheetData>"
}

private fun colLetter(zeroBasedIndex: Int): String {
    var n = zeroBasedIndex + 1
    val out = StringBuilder()
    while (n > 0) {
        val rem = (n - 1) % 26
        out.insert(0, ('A' + rem))
        n = (n - 1) / 26
    }
    return out.toString()
}

private fun xmlEsc(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&apos;")

private fun fmtNum(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

private fun worksheetXml(sheetDataXml: String): String =
    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
            sheetDataXml +
            "</worksheet>"

private const val RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

private const val STYLES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<numFmts count="1">
<numFmt numFmtId="164" formatCode="#,##0.00&quot; TL&quot;"/>
</numFmts>
<fonts count="4">
<font><sz val="11"/><name val="Calibri"/></font>
<font><b/><sz val="16"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
<font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
<font><b/><sz val="11"/><color rgb="FF333333"/><name val="Calibri"/></font>
</fonts>
<fills count="6">
<fill><patternFill patternType="none"/></fill>
<fill><patternFill patternType="gray125"/></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FF1F4E78"/><bgColor indexed="64"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FF2E75B6"/><bgColor indexed="64"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFF2F2F2"/><bgColor indexed="64"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFDDEBF7"/><bgColor indexed="64"/></patternFill></fill>
</fills>
<borders count="1">
<border><left/><right/><top/><bottom/><diagonal/></border>
</borders>
<cellStyleXfs count="1">
<xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
</cellStyleXfs>
<cellXfs count="7">
<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
<xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
<xf numFmtId="0" fontId="2" fillId="3" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
<xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
<xf numFmtId="0" fontId="3" fillId="4" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
<xf numFmtId="0" fontId="3" fillId="5" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
<xf numFmtId="164" fontId="3" fillId="5" borderId="0" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1"/>
</cellXfs>
</styleSheet>"""

private class ZipEntryData(val name: String, val data: ByteArray)

private class BytesBuilder {
    private val list = ArrayList<Byte>()
    fun u8(v: Int) { list.add((v and 0xFF).toByte()) }
    fun u16(v: Int) { u8(v); u8(v shr 8) }
    fun u32(v: Int) { u8(v); u8(v shr 8); u8(v shr 16); u8(v shr 24) }
    fun bytes(b: ByteArray) { for (x in b) list.add(x) }
    val size: Int get() = list.size
    fun toByteArray(): ByteArray = list.toByteArray()
}

private val CRC_TABLE = IntArray(256).also { t ->
    for (n in 0 until 256) {
        var c = n
        repeat(8) {
            c = if (c and 1 != 0) (c ushr 1) xor 0xEDB88320.toInt() else c ushr 1
        }
        t[n] = c
    }
}

private fun crc32(data: ByteArray): Long {
    var c = 0xFFFFFFFF.toInt()
    for (b in data) {
        c = CRC_TABLE[(c xor b.toInt()) and 0xFF] xor (c ushr 8)
    }
    return (c.toLong() xor 0xFFFFFFFFL) and 0xFFFFFFFFL
}

private fun buildZip(entries: List<ZipEntryData>): ByteArray {
    val body = BytesBuilder()
    data class LocalRef(val name: String, val crc: Long, val size: Int, val offset: Int)
    val refs = ArrayList<LocalRef>()

    for (entry in entries) {
        val nameBytes = entry.name.encodeToByteArray()
        val crc = crc32(entry.data)
        val offset = body.size
        body.u32(0x04034b50)
        body.u16(20)
        body.u16(0)
        body.u16(0)
        body.u16(0)
        body.u16(0x21)
        body.u32(crc.toInt())
        body.u32(entry.data.size)
        body.u32(entry.data.size)
        body.u16(nameBytes.size)
        body.u16(0)
        body.bytes(nameBytes)
        body.bytes(entry.data)
        refs.add(LocalRef(entry.name, crc, entry.data.size, offset))
    }

    val centralStart = body.size
    for (r in refs) {
        val nameBytes = r.name.encodeToByteArray()
        body.u32(0x02014b50)
        body.u16(20)
        body.u16(20)
        body.u16(0)
        body.u16(0)
        body.u16(0)
        body.u16(0x21)
        body.u32(r.crc.toInt())
        body.u32(r.size)
        body.u32(r.size)
        body.u16(nameBytes.size)
        body.u16(0)
        body.u16(0)
        body.u16(0)
        body.u16(0)
        body.u32(0)
        body.u32(r.offset)
        body.bytes(nameBytes)
    }
    val centralSize = body.size - centralStart

    body.u32(0x06054b50)
    body.u16(0)
    body.u16(0)
    body.u16(refs.size)
    body.u16(refs.size)
    body.u32(centralSize)
    body.u32(centralStart)
    body.u16(0)

    return body.toByteArray()
}