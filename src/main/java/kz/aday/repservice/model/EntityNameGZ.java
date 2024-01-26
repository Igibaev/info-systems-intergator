package kz.aday.repservice.model;

import lombok.Getter;

@Getter
public enum EntityNameGZ {
    Subjects("Реестр участников", "subject"),
    TrdBuy("Реестр объявлений","trd-buy"),
    Contract("Реестр договоров","contract"),
    Acts("Реестр актов","acts"),
    Lots("Реестр лотов","lots"),
    Plans("Реестр годовых планов","plans"),
    Rnu("Реестр недобросовестных поставщиков","rnu"),
    TrdApp("Заявки поставщиков","trd-app");

    private String nameRu;
    private String name;

    EntityNameGZ(String nameRu, String name) {
        this.nameRu = nameRu;
        this.name = name;
    }
}
