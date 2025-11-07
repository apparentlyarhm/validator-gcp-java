        package com.arhum.validator.model.rcon;

        import com.arhum.validator.model.enums.RconCommands;
        import jakarta.validation.constraints.NotEmpty;
        import jakarta.validation.constraints.NotNull;
        import lombok.Getter;
        import lombok.Setter;

        import java.util.List;

        @Getter
        @Setter
        public class RconRequest {

            @NotEmpty(message = "Command name cannot be empty.")
            private RconCommands command;

            @NotNull(message = "Arguments list cannot be null (use an empty list for no arguments).")
            private List<String> arguments;
        }
