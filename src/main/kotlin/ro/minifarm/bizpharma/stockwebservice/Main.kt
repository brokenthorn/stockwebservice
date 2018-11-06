package ro.minifarm.bizpharma.stockwebservice

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.basic
import io.ktor.features.*
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.slf4j.event.Level
import java.math.BigDecimal
import java.text.DateFormat
import java.util.*
import java.util.concurrent.Executors

// Set up a new fixed thread pool context for coroutines that will query the database.
// TODO: unless properly closed `databaseQueryCoroutineDispatcher` leaks threads so this should probably be closed when the server closes or terminates abnormally

val databaseQueryCoroutineDispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()

// This is a generic function that executes a block of code with a coroutine dispatcher, asynchronously:
suspend fun <T> launchDbQuery(block: () -> T): Deferred<T> {
    return withContext(databaseQueryCoroutineDispatcher) {
        async { block() }
    }
}

// This suspendable function gets the current stock list:
suspend fun getStock(dataSource: HikariDataSource): Deferred<List<StockLineItem>> = launchDbQuery {
    val connection = dataSource.connection
    val resultSet = connection
            .prepareStatement("with rezumat_stoc_cte(location_id, article_id, acquisition_date, best_before_date, lot, net_acquisition_price, gross_acquisition_price, added_value_percent, retail_price, vat_percent, fractions, whole_quantity, fraction_quantity) as (select IdLocatie as location_id, IdArticol as article_id, DataIntrare as acquisition_date, BBD as best_before_date, Lot as lot, cast(PretAchizitie as decimal(8, 2)) as net_acquisition_price, cast(PretAchizitieNediscFTVA as decimal(8, 2)) as gross_acquisition_price, ProcAdaos as added_value_percent, cast(PretAmanunt as decimal(8, 2)) as retail_price, cast(ProcTVA as decimal(4, 2)) as vat_percent, CantNrBuc as fractions, SUM(CantI) as whole_quantity, SUM(CantF) as fraction_quantity from BizPharmaHO.dbo.Stoc group by IdLocatie, IdArticol, BBD, Lot, PretAchizitie, PretAchizitieNediscFTVA, PretAmanunt, ProcTVA, ProcAdaos, DataIntrare, CantNrBuc) select location_id, article_id, acquisition_date, best_before_date, lot, net_acquisition_price, gross_acquisition_price, added_value_percent, retail_price, vat_percent, fractions, case when fraction_quantity < fractions then whole_quantity else whole_quantity + floor(fraction_quantity / fractions) end as whole_quantity, case when fraction_quantity < fractions then fraction_quantity else fraction_quantity - (floor(fraction_quantity / fractions) * fractions) end as fraction_quantity from rezumat_stoc_cte;")
            .executeQuery()
    val stock: MutableList<StockLineItem> = ArrayList()

    while (resultSet.next()) {
        stock.add(StockLineItem(
                location_id = resultSet.getInt("location_id"),
                article_id = resultSet.getInt("article_id"),
                whole_quantity = resultSet.getInt("whole_quantity"),
                vat_percent = resultSet.getBigDecimal("vat_percent"),
                retail_price = resultSet.getBigDecimal("retail_price"),
                lot = resultSet.getString("lot"),
                net_acquisition_price = resultSet.getBigDecimal("net_acquisition_price") ?: BigDecimal(0),
                gross_acquisition_price = resultSet.getBigDecimal("gross_acquisition_price") ?: BigDecimal(0),
                fractions = resultSet.getInt("fractions"),
                fraction_quantity = resultSet.getInt("fraction_quantity"),
                best_before_date = resultSet.getDate("best_before_date"),
                added_value_percent = resultSet.getBigDecimal("added_value_percent"),
                acquisition_date = resultSet.getDate("acquisition_date")
        ))
    }

    resultSet.close()
    connection.rollback()
    connection.close()

    return@launchDbQuery stock
}

// This function creates a Hikari Data Source, a database connection pool:
fun createHikariDataSource(): HikariDataSource {
    val config = HikariConfig()
    config.jdbcUrl = System.getenv("JDBC_URL") ?: "jdbc:sqlserver://ho.minifarm.ro\\SQL2008:1443;database=BizPharmaHO"
    config.username = System.getenv("JDBC_USERNAME") ?: "sa"
    config.password = System.getenv("JDBC_PASSWORD") ?: ""
    config.maxLifetime = 60_000 // 1 minute
    config.maximumPoolSize = 4
    config.isAutoCommit = false
    config.isReadOnly = true
    config.addDataSourceProperty("cachePrepStmts", "true")
    config.addDataSourceProperty("prepStmtCacheSize", "250")
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    config.validate()
    return HikariDataSource(config)
}

fun getApiAuthPassword(): String? = System.getenv("API_PASSWORD")

fun Application.module() {
    val data_source = createHikariDataSource()

    install(DefaultHeaders)
    install(CallLogging) { level = Level.DEBUG }
    install(Compression) { deflate { minimumSize(1024) }; gzip { minimumSize(1024) } }
    install(ContentNegotiation) {
        jackson { dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale("ro", "ro")) }
    }
    install(Authentication) {
        basic(name = "authenticated-clients") {
            realm = "stockwebservice"
            validate { credentials ->
                val pwd = getApiAuthPassword()
                if (pwd.isNullOrBlank()) null
                else if (credentials.password == pwd) UserIdPrincipal(credentials.name)
                else null
            }
        }
    }

    install(Routing) {
        get("/ping") {
            call.respondText { "pong!" }
        }
        authenticate("authenticated-clients") {
            get("/stock") {
                call.respond(getStock(data_source).await())
            }
        }
    }
}

fun main(args: Array<String>) {
    embeddedServer(Netty,
            8080,
            //watchPaths = listOf("stockwebservice"),
            module = Application::module
    ).start(wait = true)
}