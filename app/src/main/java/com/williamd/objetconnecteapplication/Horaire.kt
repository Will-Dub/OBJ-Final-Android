package com.williamd.objetconnecteapplication

import java.io.Serializable
import java.util.UUID

class Horaire(val debut:String, val type: HoraireTypeEnum, val id: String = UUID.randomUUID().toString()): Serializable {
}


enum class HoraireTypeEnum {
    ALLUME,
    ETEINT;
}
