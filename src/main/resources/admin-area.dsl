import com.demo.dto.AdminUser;

/** Operacje tylko dla administratorów */
trait AdminOps<T extends AdminUser> {
    @Deprecated                // adnotacja na endpoint
    GET "/"
}

/* ---------- blok zasobów chronionych ---------- */
@Secured("ADMIN")              // adnotacja na resource
resource "/admin" {
    GET "/**" status 200                     // wildcard path + status
    resource "/users" {

        /* transactional + explicit returns void */
        POST "/" expects AdminUser transactional returns void
    }
}
