import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AcceptedIdea, DraftKind, IdeaDraft, IdeaView } from '../../../models/idea-view';
import { IdeaService } from '../idea.service';

/** The kinds in the order they are listed, with how each is headed. */
const KINDS: { kind: DraftKind; heading: string }[] = [
  { kind: 'ACTOR', heading: 'Actors' },
  { kind: 'GOAL', heading: 'Goals' },
  { kind: 'GLOSSARY_TERM', heading: 'Glossary terms' },
  { kind: 'STORY', heading: 'Stories' },
  { kind: 'USE_CASE', heading: 'Use cases' },
];

const POLL_MS = 2000;

/**
 * Review of the requirements proposed for an idea. Polls while the proposals are being
 * generated, then lists them to choose from; the chosen ones become the new project's
 * requirements.
 */
@Component({
  selector: 'app-idea-review',
  templateUrl: './idea-review.component.html',
  styleUrls: ['./idea-review.component.scss'],
})
export class IdeaReviewComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ideas = inject(IdeaService);

  projectName = '';
  ideaId = '';

  readonly idea = signal<IdeaView | null>(null);
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);
  readonly accepted = signal<AcceptedIdea | null>(null);
  /** Positions in `idea().drafts` that are ticked. */
  readonly selected = signal<ReadonlySet<number>>(new Set());

  readonly working = computed(() => {
    const status = this.idea()?.status;
    return status === 'PENDING' || status === 'GENERATING';
  });

  /** The drafts by kind, each with its position in the idea's list. */
  readonly groups = computed(() => {
    const drafts = this.idea()?.drafts ?? [];
    return KINDS.map(({ kind, heading }) => ({
      heading,
      items: drafts
        .map((draft, index) => ({ draft, index }))
        .filter(item => item.draft.kind === kind),
    })).filter(group => group.items.length > 0);
  });

  private timer: ReturnType<typeof setTimeout> | null = null;
  private destroyed = false;

  ngOnInit(): void {
    this.projectName = this.route.snapshot.paramMap.get('project') ?? '';
    this.ideaId = this.route.snapshot.paramMap.get('id') ?? '';
    void this.load();
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    if (this.timer) {
      clearTimeout(this.timer);
    }
  }

  private async load(): Promise<void> {
    try {
      const idea = await this.ideas.getIdea(this.projectName, this.ideaId);
      if (this.destroyed) {
        return;
      }
      const first = this.idea() === null || this.idea()!.status !== 'DRAFTED';
      this.idea.set(idea);
      this.error.set(null);
      if (idea.status === 'DRAFTED' && first) {
        // everything starts ticked: the manager unticks what to leave out
        this.selected.set(new Set(idea.drafts.map((_, index) => index)));
      }
      if (this.working()) {
        this.timer = setTimeout(() => void this.load(), POLL_MS);
      }
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'Could not load the idea.');
    }
  }

  isSelected(index: number): boolean {
    return this.selected().has(index);
  }

  toggle(index: number): void {
    const next = new Set(this.selected());
    if (!next.delete(index)) {
      next.add(index);
    }
    this.selected.set(next);
  }

  selectAll(all: boolean): void {
    this.selected.set(all ? new Set((this.idea()?.drafts ?? []).map((_, i) => i)) : new Set());
  }

  confidenceLabel(draft: IdeaDraft): string | null {
    return draft.confidence == null ? null : `${Math.round(draft.confidence * 100)}% sure`;
  }

  async retry(): Promise<void> {
    this.busy.set(true);
    try {
      await this.ideas.retry(this.projectName, this.ideaId);
      this.error.set(null);
      await this.load();
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'Could not retry.');
    } finally {
      this.busy.set(false);
    }
  }

  async accept(): Promise<void> {
    if (this.selected().size === 0 || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      this.accepted.set(
        await this.ideas.accept(this.projectName, this.ideaId, [...this.selected()].sort((a, b) => a - b)));
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'Could not create the requirements.');
    } finally {
      this.busy.set(false);
    }
  }

  openProject(): void {
    void this.router.navigate(['/projects', this.projectName]);
  }

  createdSummary(accepted: AcceptedIdea): string {
    return Object.entries(accepted.created).map(([kind, n]) => `${n} ${kind}${n === 1 ? '' : 's'}`).join(', ');
  }
}
