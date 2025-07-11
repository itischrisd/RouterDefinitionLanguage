package com.todo.api;

/* ---------- pojedynczy trait ---------- */
trait Crud<Item> {
    GET "/"                               // lista
    GET "/{id}"        (id: Long)         // pojedynczy
    POST "/"           expects Item
    DELETE "/{id}"     (id: Long) returns void
}

/* ---------- pojedynczy zasób ---------- */
resource "/items" {
    uses Crud<Item>

    // dodatkowy endpoint poza traitem
    GET "/search" (q: String)
}
