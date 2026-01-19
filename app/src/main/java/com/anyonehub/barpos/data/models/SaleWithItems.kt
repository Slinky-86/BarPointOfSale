/* Copyright 2024 anyone-Hub */
package com.anyonehub.barpos.data.models

import androidx.room.Embedded
import androidx.room.Relation
import com.anyonehub.barpos.data.SaleItem
import com.anyonehub.barpos.data.Sale

data class SaleWithItems(
    @Embedded val sale: Sale,
    @Relation(
        parentColumn = "id",
        entityColumn = "sale_id"
    )
    val items: List<SaleItem>
)
