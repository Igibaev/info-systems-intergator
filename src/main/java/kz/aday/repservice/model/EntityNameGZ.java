package kz.aday.repservice.model;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum EntityNameGZ {
    Subjects("Реестр участников", "subject"),
    TrdBuy("Реестр объявлений", "trd-buy"),
    Contract("Реестр договоров", "contract"),
    Acts("Реестр актов", "acts"),
    Lots("Реестр лотов", "lots"),
    Plans("Реестр годовых планов", "plans/all"),
    Rnu("Реестр недобросовестных поставщиков", "rnu"),
    Ref_kato("Справочник КАТО", "refs/ref_kato"),
    Ref_lots_status("Справочник Статусы лотов", "refs/ref_lots_status"),
    Ref_enstru("Справочник ЕНС ТРУ", "refs/ref_enstru"),
    Ref_trade_methods("Справочник Способ закупки", "refs/ref_trade_methods"),
    Ref_units("Справочник МКЕЙ", "refs/ref_units"),
    Ref_months("Справочник Месяца", "refs/ref_months"),
    Ref_pln_point_status("Справочник Статусы пуктов планов", "refs/ref_pln_point_status"),
    Ref_subject_type("Справочник Вид предмета закупки", "refs/ref_subject_type"),
    Ref_finsource("Справочник Источник финансирования", "refs/ref_finsource"),
    Ref_abp("Справочник Администратор бюджетной программы", "refs/ref_abp"),
    Ref_point_type("Справочник Тип пункта плана", "refs/ref_point_type"),
    Ref_countries("Справочник Страны", "refs/ref_countries"),
    Ref_ekrb("Справочник специфик", "refs/ref_ekrb"),
    Ref_fkrb_program("Справочник программ ФКР", "refs/ref_fkrb_program"),
    Ref_fkrb_subprogram("Справочник подпрограмм ФКР", "refs/ref_fkrb_subprogram"),
    Ref_justification("Справочник Обоснование применения способа закупки", "refs/ref_justification"),
    Ref_amendment_agreem_type("Справочник Вид дополнительного соглашения", "refs/ref_amendment_agreem_type"),
    Ref_amendm_agreem_justif("Справочник Основания создания дополнительного соглашения", "refs/ref_amendm_agreem_justif"),
    Ref_budget_type("Справочник Вид бюджета", "refs/ref_budget_type"),
    Ref_type_trade("Справочник Тип закупки", "refs/ref_type_trade"),
    Ref_buy_status("Справочник Статус объявления", "refs/ref_buy_status"),
    Ref_po_st("Справочник Статусы ценовых предложений", "refs/ref_po_st"),
    Ref_comm_roles("Справочник Роль члена комиссии", "refs/ref_comm_roles"),
    Ref_contract_status("Справочник Статус договора", "refs/ref_contract_status"),
    Ref_contract_agr_form("Справочник форм заключения договора", "refs/ref_contract_agr_form"),
    Ref_contract_year_type("Справочник Тип договора (однолетний/многолетний)", "refs/ref_contract_year_type"),
    Ref_currency("Справочник валют", "refs/ref_currency"),
    Ref_contract_cancel("Справочник статей для расторжения договора", "refs/ref_contract_cancel"),
    Ref_contract_type("Справочник типов договора", "refs/ref_contract_type"),
    Ref_reason("Справочник причин внесения в РНУ", "refs/ref_reason"),
    Ref_buy_lot_reject_reason("Справочник Список причин по которым не состоялся аукцион по лоту", "refs/ref_buy_lot_reject_reason");

    private String nameRu;
    private String name;
    private static Map<String, EntityNameGZ> map = new HashMap<>();

    static {
        for (EntityNameGZ entityNameGZ: EntityNameGZ.values()) {
            map.put(entityNameGZ.name, entityNameGZ);
        }
    }

    EntityNameGZ(String nameRu, String name) {
        this.nameRu = nameRu;
        this.name = name;
    }

    public static EntityNameGZ getByEntityName(String name) {
        if (map.containsKey(name)) {
            return map.get(name);
        }
        return null;
    }
}
