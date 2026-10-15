/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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
import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { DeleteProjectDialogComponent } from './delete-project-dialog';
import { ProjectService } from '../../core/project.service';
import { CommandService } from '../../core/command.service';

describe('DeleteProjectDialogComponent', () => {
  let projectServiceMock: {
    downloadProjectXml: ReturnType<typeof vi.fn>;
    notifyTreeChanged: ReturnType<typeof vi.fn>;
  };
  let commandServiceMock: { execute: ReturnType<typeof vi.fn> };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let fixture: any;
  let comp: DeleteProjectDialogComponent;

  beforeEach(() => {
    // jsdom does not implement the blob-download plumbing; stub it so saveBlob() runs.
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (URL as any).createObjectURL = vi.fn(() => 'blob:mock');
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (URL as any).revokeObjectURL = vi.fn();

    projectServiceMock = {
      downloadProjectXml: vi.fn().mockResolvedValue(new Blob(['<project/>'], { type: 'application/xml' })),
      notifyTreeChanged: vi.fn(),
    };
    commandServiceMock = { execute: vi.fn().mockResolvedValue({ success: true }) };

    TestBed.configureTestingModule({
      imports: [DeleteProjectDialogComponent],
      providers: [
        provideNoopAnimations(),
        { provide: ProjectService, useValue: projectServiceMock },
        { provide: CommandService, useValue: commandServiceMock },
      ],
    });

    fixture = TestBed.createComponent(DeleteProjectDialogComponent);
    comp = fixture.componentInstance;
    comp.project = { name: 'Acme', version: 3 };
    comp.visible = true; // triggers reset()
    fixture.detectChanges();
  });

  it('defaults to export-first on and keeps Delete disabled until the export resolves', async () => {
    expect(comp.exportFirst()).toBe(true);
    expect(comp.exported()).toBe(false);
    expect(comp.canConfirm()).toBe(false);

    await comp.onExport();

    expect(projectServiceMock.downloadProjectXml).toHaveBeenCalledWith('Acme');
    expect(comp.exported()).toBe(true);
    expect(comp.canConfirm()).toBe(true);
  });

  it('enables Delete immediately when export-first is turned off', () => {
    comp.onExportFirstChange(false);
    expect(comp.canConfirm()).toBe(true);
  });

  it('re-requires a fresh backup when export-first is toggled back on', async () => {
    await comp.onExport();
    expect(comp.canConfirm()).toBe(true);

    comp.onExportFirstChange(false);
    comp.onExportFirstChange(true);

    expect(comp.exported()).toBe(false);
    expect(comp.canConfirm()).toBe(false);
  });

  it('confirm dispatches DeleteProject with name+version, notifies the tree, emits deleted, and closes', async () => {
    const deleted = vi.fn();
    comp.deleted.subscribe(deleted);
    comp.onExportFirstChange(false); // skip the backup for this case

    await comp.onConfirm();

    expect(commandServiceMock.execute).toHaveBeenCalledWith('DeleteProject', { projectName: 'Acme', version: 3 });
    expect(projectServiceMock.notifyTreeChanged).toHaveBeenCalledTimes(1);
    expect(deleted).toHaveBeenCalledTimes(1);
    expect(comp.visible).toBe(false);
  });

  it('does not dispatch while a required backup has not been taken', async () => {
    // export-first on and not yet exported => canConfirm() is false
    await comp.onConfirm();
    expect(commandServiceMock.execute).not.toHaveBeenCalled();
    expect(comp.visible).toBe(true);
  });

  it('surfaces a command failure inline and does not emit deleted', async () => {
    commandServiceMock.execute.mockResolvedValue({ success: false, error: 'Boom' });
    const deleted = vi.fn();
    comp.deleted.subscribe(deleted);
    comp.onExportFirstChange(false);

    await comp.onConfirm();

    expect(comp.errorMessage()).toBe('Boom');
    expect(deleted).not.toHaveBeenCalled();
    expect(comp.visible).toBe(true); // stays open so the user can retry or cancel
  });

  it('surfaces an export failure and leaves Delete gated', async () => {
    projectServiceMock.downloadProjectXml.mockRejectedValue(new Error('net down'));

    await comp.onExport();

    expect(comp.errorMessage()).toBe('net down');
    expect(comp.exported()).toBe(false);
    expect(comp.canConfirm()).toBe(false);
    expect(commandServiceMock.execute).not.toHaveBeenCalled();
  });

  it('cancel closes without dispatching', () => {
    const visibleChanges = vi.fn();
    comp.visibleChange.subscribe(visibleChanges);

    comp.onCancel();

    expect(commandServiceMock.execute).not.toHaveBeenCalled();
    expect(comp.visible).toBe(false);
    expect(visibleChanges).toHaveBeenCalledWith(false);
  });

  it('reopening the dialog resets export state', async () => {
    await comp.onExport();
    expect(comp.exported()).toBe(true);

    comp.visible = false;
    comp.visible = true; // reopen -> reset()

    expect(comp.exported()).toBe(false);
    expect(comp.exportFirst()).toBe(true);
    expect(comp.errorMessage()).toBeNull();
  });
});
