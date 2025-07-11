#var API_VERSION "v2"           // makro do podstawień

package org.example.api;
import org.example.dto;

 /** Ogólny CRUD z pagingiem */
trait CRUD<T, ID> {
    GET  "/"                    returns java.util.List<T>
    GET  "/{id}"   (id: ID)     returns T
    POST "/"       expects T  status 201 returns T
    PUT  "/{id}"   (id: ID, body: T) expects T returns T
    DELETE "/{id}" (id: ID)    // returns void (implicit)
}

/** Trait dodający stronicowanie */
trait Pageable<T> {
    GET "/page" returns Page<T>
}

/** Przykład override */
trait UserCustom {
    override GET "/{id}" (id: Long) returns User
}

/* ---------- główny resource ---------- */
@Tag(key="users")                         // adnotacja z arg‑kv
resource "/${API_VERSION}/users" (filter?: String) {

    uses CRUD<User, Long>
    uses Pageable<User>
    uses UserCustom

    // wielokrotny path w jednej linii
    GET "/stats", "/stats/{year}" (year?: Int) returns Stats

    @Secured("USER")
    resource "/{userId}" (userId: Long) {

        GET "/" returns User

        /* zagnieżdżone zasoby – posts w ramach usera */
        resource "/posts" {
            GET "/"  returns java.util.List<Post>

            @Transactional
            POST "/" expects Post returns Post

            resource "/{postId}" (postId: Long) {
                GET "/" returns Post
            }
        }
    }
}
