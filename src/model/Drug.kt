package repo

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder

@Serializable
class Drug (
    override var id: Int = -1,
    val workerId: Int,
    val title: String,
    val description: String,
    val price: Int
    //val inside: Int
) : Item

class DrugsTable : ItemTable<Drug>() {
    val workerId = integer("workerId").references(workersTable.id, onDelete = ReferenceOption.CASCADE)
    val title = varchar("title", 50)
    val description = varchar("description", 500)
    val price = integer("price")
    //val inside: Column<Boolean> = bool("inside")
    override fun fill(builder: UpdateBuilder<Int>, item: Drug) {
        builder[workerId] = item.workerId
        builder[title] = item.title
        builder[description] = item.description
        builder[price] = item.price
       // builder[inside] = item.inside
    }
    override fun readResult(result: ResultRow) =
        Drug(
            result[id].value,
            result[workerId],
            result[title],
            result[description],
            result[price]
            //result[inside]
        )
}

val drugsTable = DrugsTable()