package com.rds.questlog.data.local

/** Meng-escape `\`, `%`, dan `_` agar teks pengguna dicocokkan apa adanya oleh `LIKE ... ESCAPE '\'`. */
fun escapeLike(query: String): String = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
