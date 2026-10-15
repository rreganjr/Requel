/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.rreganjr.command;

import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.repository.jpa.ExceptionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ExceptionMappingCommandHandler}.
 *
 * The handler is a thin decorator: on success it returns the command unchanged;
 * on exception it calls {@link ExceptionMapper#convertException(Exception)} and
 * throws the result. {@link ExceptionMapper} is mocked so these tests verify
 * delegation only — mapping logic is covered by {@code ExceptionMapperTest}.
 *
 * Scenarios covered:
 * - Delegate succeeds: result returned, exceptionMapper never called
 * - Delegate throws checked exception: convertException called, result re-thrown
 * - convertException returns a different RuntimeException: that exception is thrown
 */
class ExceptionMappingCommandHandlerTest {

    private ExceptionMapper exceptionMapper;
    private CommandHandler delegate;
    private ExceptionMappingCommandHandler handler;

    @BeforeEach
    void setUp() {
        exceptionMapper = mock(ExceptionMapper.class);
        delegate = mock(CommandHandler.class);
        handler = new ExceptionMappingCommandHandler(exceptionMapper, delegate);
    }

    @Test
    void successfulExecutionReturnsDelegateResult() throws Exception {
        Command cmd = mock(Command.class);
        when(delegate.execute(cmd)).thenReturn(cmd);

        Command result = handler.execute(cmd);

        assertThat(result).isSameAs(cmd);
        verifyNoInteractions(exceptionMapper);
    }

    @Test
    void delegateExceptionIsConvertedAndRethrown() throws Exception {
        Command cmd = mock(Command.class);
        RuntimeException original = new RuntimeException("db error");
        EntityException converted = EntityException.uniquenessConflict(null, null, null, null);

        when(delegate.execute(cmd)).thenThrow(original);
        when(exceptionMapper.convertException(original)).thenReturn(converted);

        assertThatThrownBy(() -> handler.execute(cmd))
                .isSameAs(converted);

        verify(exceptionMapper).convertException(original);
    }

    @Test
    void convertedExceptionIsWhatGetsThrown() throws Exception {
        Command cmd = mock(Command.class);
        Exception original = new Exception("checked exception");
        RuntimeException mapped = new IllegalStateException("mapped");

        when(delegate.execute(cmd)).thenThrow(original);
        when(exceptionMapper.convertException(original)).thenReturn(mapped);

        assertThatThrownBy(() -> handler.execute(cmd))
                .isSameAs(mapped);
    }
}
