# HomeAutomationDummy
This dummy simulates a few devices and sensors sending messages via MQTT to a local MQTT message broker on port 1883.

## Messages sent by the Dummy

|Topic|Message low er limit|Message upper limit|average step size|deviation|
|---|---|---|---|---|
|/sensor/temperature1|17|30|.2|.1|
|/sensor/temperature1_2|17|30|.2|.05|
|/sensor/temperature2|20|33|.2|.1|
|/sensor/humidity1|5|70|3|1|
|/sensor/humidity2|5|70|3|1|
|/sensor/humidity3|5|70|3|1|
|/sensor/co2_1|400|1800|80|20|
|/sensor/co2_2|400|1800|80|20|
|/sensor/co2_3|400|1800|80|20|
|/sensor/light_level_outside|0|20|1|.5|

In addition, the RF Codes 4414, 4415, 4424, 4425, 4434, 4435, 4444, 4445 and 4315 are sent one after another on channel /sensor/rf.

A new value is sent after an initial delay of 0 - 5 seconds every 2.5 seconds until the program is stopped by a user.

