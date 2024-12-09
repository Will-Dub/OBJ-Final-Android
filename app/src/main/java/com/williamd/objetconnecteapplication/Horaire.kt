package com.williamd.objetconnecteapplication

class Horaire(val debut:String, val type: HoraireTypeEnum) {
}


enum class HoraireTypeEnum {
    ALLUME,
    ETEINT;
}
