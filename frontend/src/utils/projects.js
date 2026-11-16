export const DESIGNATIONS = [
    ['HEAD_OF_DEPARTMENT', 'Head of Department'],
    ['PROFESSOR', 'Professor'],
    ['ASSOCIATE_PROFESSOR', 'Associate Professor'],
    ['SENIOR_LECTURER', 'Senior Lecturer'],
    ['LECTURER_I', 'Lecturer I'],
    ['LECTURER_II', 'Lecturer II'],
    ['ASSISTANT_LECTURER', 'Assistant Lecturer'],
    ['ADJUNCT', 'Adjunct Lecturer'],
]

export const designationLabel = (code) => (DESIGNATIONS.find(([c]) => c === code) || [code, code || ''])[1]

export const CLASS_LABEL = { FIRST: 'First Class', UPPER: 'Second Class Upper', LOWER: 'Second Class Lower', THIRD: 'Third Class', OTHER: 'Unclassified' }
export const classLabel = (code) => CLASS_LABEL[code] || code

export const CLASS_STYLE = {
    FIRST: 'bg-green-100 text-green-700',
    UPPER: 'bg-blue-100 text-blue-700',
    LOWER: 'bg-indigo-100 text-indigo-700',
    THIRD: 'bg-amber-100 text-amber-700',
    OTHER: 'bg-slate-200 text-slate-600',
}