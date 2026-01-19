# Copyright 2024 anyone-Hub
import simplejson as json
import pendulum
from moneyed import Money, USD
from pydantic import BaseModel, Field
from typing import Dict, List
from babel.numbers import format_currency

class MenuConfig(BaseModel):
    taxRate: float = 0.08
    happyHourStart: int = 16
    happyHourEnd: int = 18
    happyHourDiscount: float = 0.50
    specials: Dict[str, float] = {}

class SaleItem(BaseModel):
    id: int
    name: str = "Item"
    price: float
    inventory_count: int = 999
    is_shot_wall: bool = False

def calculate_sale(item_json_str: str, config_json_str: str) -> str:
    """
    Calculates unit price based on hierarchy: Special -> Happy Hour -> Standard.
    Used for adding items to tabs locally.
    """
    try:
        item_data = SaleItem.parse_raw(item_json_str)
        config = MenuConfig.parse_raw(config_json_str)

        if item_data.is_shot_wall and item_data.inventory_count <= 0:
            return json.dumps({
                "unitPrice": 0.00,
                "status": "out_of_stock"
            })

        base_price = Money(item_data.price, USD)
        now = pendulum.now()

        # Happy Hour logic
        is_hh = config.happyHourStart <= now.hour < config.happyHourEnd

        item_id_str = str(item_data.id)
        if item_id_str in config.specials:
            final_price = Money(config.specials[item_id_str], USD)
            rule = "special"
        elif is_hh:
            final_price = base_price * (1.0 - config.happyHourDiscount)
            rule = "happy_hour"
        else:
            final_price = base_price
            rule = "standard"

        return json.dumps({
            "unitPrice": float(final_price.amount),
            "formattedPrice": format_currency(final_price.amount, 'USD', locale='en_US'),
            "rule": rule,
            "status": "success"
        })
    except Exception as e:
        return json.dumps({"status": "error", "message": str(e)})

def calculate_transaction(items_json_str: str, config_json_str: str) -> str:
    """
    Calculates the final totals for a tab (cashing out).
    Ensures math is consistent with local tax rules.
    """
    try:
        items = json.loads(items_json_str) # List of dicts with 'price' and 'qty'
        config = MenuConfig.parse_raw(config_json_str)
        
        subtotal = Money(0, USD)
        for item in items:
            price = item.get("price", 0.0)
            qty = item.get("qty", 1)
            subtotal += Money(price, USD) * qty
            
        tax_amount = subtotal * config.taxRate
        grand_total = subtotal + tax_amount
        
        return json.dumps({
            "subtotal": float(subtotal.amount),
            "tax": float(tax_amount.amount),
            "total": float(grand_total.amount),
            "formattedTotal": format_currency(grand_total.amount, 'USD', locale='en_US'),
            "status": "success"
        })
    except Exception as e:
        return json.dumps({"status": "error", "message": str(e)})

def generate_z_report(sales_json_str: str) -> str:
    """
    Processes all sales for the shift to generate a final audit.
    Matched to ZReportManager.kt
    """
    try:
        sales_list = json.loads(sales_json_str)
        total_revenue = Money(0, USD)
        item_counts = {}

        for sale in sales_list:
            amount = Money(sale.get("price", 0), USD)
            total_revenue += amount
            name = sale.get("name", "Unknown Item")
            item_counts[name] = item_counts.get(name, 0) + 1

        return json.dumps({
            "total_raw": str(total_revenue.amount),
            "total_formatted": format_currency(total_revenue.amount, 'USD', locale='en_US'),
            "item_counts": item_counts,
            "status": "success",
            "timestamp": pendulum.now().to_iso8601_string()
        })
    except Exception as e:
        return json.dumps({"status": "error", "message": str(e)})
