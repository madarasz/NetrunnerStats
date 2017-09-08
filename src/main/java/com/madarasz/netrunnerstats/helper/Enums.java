package com.madarasz.netrunnerstats.helper;

import org.springframework.stereotype.Component;

/**
 * Created by madarasz on 11/7/15.
 * Enums.
 */
@Component
public class Enums {

    public enum Factions {
        neutral, shaper, criminal, anarch, jinteki, haas_bioroid, weyland_consortium, nbn;
    }

    public enum CardCycles {
        Promos(0), Core_Set(1), Genesis(2), Creation_and_Control(3), Spin(4), Honor_and_Profit(5), Lunar(6),
        Order_and_Chaos(7), SanSan(8), Data_and_Destiny(9), Mumbad(10), Flashpoint(11), Red_Sand(12),
        Terminal_Directive(13), Red_Sand_Part_2(14);

        private final int cycleNumber;

        CardCycles(int cycleNumber) {
            this.cycleNumber = cycleNumber;
        }

        public int getCycleNumber() {
            return this.cycleNumber;
        }
    }
}
