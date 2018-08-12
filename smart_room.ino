

int bulb1 = 13;
char val;

void setup() {
  pinMode(bulb1, OUTPUT);
  Serial.begin(9600);
}

void loop() {
  if (Serial.available()){
    val = Serial.read();  
  }
  if (val == 'l'){
    digitalWrite(bulb1, HIGH);
  }
  if (val == 'h'){
    digitalWrite(bulb1, LOW);
  }
  delay(100);
}
