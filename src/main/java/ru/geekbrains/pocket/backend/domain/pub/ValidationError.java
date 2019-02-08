package ru.geekbrains.pocket.backend.domain.pub;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ValidationError {

    private String fieldName;

    @Override
    public String toString() {
        return "UserBlacklist{" +
                "'fieldName':'" + fieldName + "'" +
                '}';
    }

}