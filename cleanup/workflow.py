"""Reviewed cleanup operations over the existing exact-state recovery engine."""
import json
from pathlib import Path
import threading
import uuid
from .core import Refused, Session, private_directory, review, selection
from .inventory import Executor

PROFILES=Path(__file__).parent/'profiles'

class Workflow:
    def __init__(self, transport, data, profiles=None, executor=None):
        self.transport=transport
        self.primary=private_directory(Path(data)/'sessions')
        self.mirror=private_directory(Path(data)/'recovery-copy')
        self.profiles=profiles if profiles is not None else [json.loads(p.read_text()) for p in PROFILES.glob('*.json')]
        self.executor=executor or (lambda device:Executor(transport,device,self.profiles))
        self.pending={};self.stop_event=threading.Event()
    def inspect(self, device):
        snapshot=self.executor(device).inspect()
        result=review(snapshot,self.profiles)
        return dict(result,device_id=device,package_count=len(snapshot['packages']),apply_available=False)
    def prepare(self, device, chosen):
        if not isinstance(chosen,list) or not all(isinstance(name,str) for name in chosen):raise Refused('Choose reviewed package names')
        snapshot=self.executor(device).inspect()
        plan=selection(snapshot,self.profiles,chosen)
        session=Session(self.primary,self.mirror);session.prepare(snapshot,plan)
        token=str(uuid.uuid4());self.pending[token]=(device,plan,session.id)
        # An old review cannot accumulate indefinitely or become a replay.
        self.pending={token:self.pending[token]}
        return dict(status='confirm',token=token,device=plan['device'],selected=plan['selected'],
                    confirmation=plan['confirmation'],expires_in=120,session=session.id,recovery_verified=True)
    def apply(self, token, confirmation):
        if not isinstance(token,str) or token not in self.pending:raise Refused('Review and confirm the selected changes again')
        device,plan,session_id=self.pending.pop(token)
        if confirmation!=plan['confirmation']:raise Refused('Confirmation does not match this review')
        session=Session(self.primary,self.mirror,session_id)
        try:
            result=session.apply(self.executor(device),self.profiles,plan,confirmation,stop=self.stop_event.is_set)
            return dict(status='results',session=session.id,record=result)
        except Exception:
            # A failure before any mutation may still have prepared a durable
            # record; expose it through session listing rather than blindly retry.
            raise
    def sessions(self):
        found=[]
        for path in sorted(self.primary.glob('*.json')):
            session=Session(self.primary,self.mirror,path.stem)
            try:
                record=session.load()
                found.append(dict(session=session.id,status=record['status'],device=record['baseline']['device'],items=record['items']))
            except (ValueError,OSError,KeyError):
                found.append(dict(session=path.stem,status='recovery-needs-attention'))
        return found
    def restore(self, device, session_id, chosen=None):
        session=Session(self.primary,self.mirror,session_id)
        return dict(status='results',session=session.id,record=session.restore(self.executor(device),chosen))
    def check(self, device, session_id):
        session=Session(self.primary,self.mirror,session_id);record=session.load()
        current=self.executor(device).inspect()
        if current['device']!=record['baseline']['device'] or current['home']!=record['baseline']['home']:
            raise Refused('Reconnect to the original TV and Home before checking results')
        items={}
        for name,item in record['items'].items():
            actual=current['packages'].get(name);before=record['baseline']['packages'][name]
            if not actual or any(actual[k]!=before[k] for k in ('uid','version')):state='unresolved'
            elif actual['enabled']==item['before']:state='original'
            elif actual['enabled']==3:state='disabled'
            else:state='unresolved'
            items[name]=state
        return dict(status='checked',session=session.id,items=items)
    def export(self, session_id):
        session=Session(self.primary,self.mirror,session_id)
        return session.load()
